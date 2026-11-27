package com.island.phone.net.adapter

import com.island.phone.net.ApiAdapter
import com.island.phone.net.ApiConfig
import com.island.phone.net.ChatEvent
import com.island.phone.net.ChatRequest
import com.island.phone.net.ChatRole
import com.island.phone.net.buildPost
import com.island.phone.net.errorFromJson
import com.island.phone.net.parseJson
import com.island.phone.net.textOrNull
import com.island.phone.net.trimBase
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

internal class ClaudeAdapter : ApiAdapter {

    override fun buildRequest(config: ApiConfig, request: ChatRequest, stream: Boolean): Request {
        val systemText = (listOfNotNull(request.system) +
            request.messages.filter { it.role == ChatRole.SYSTEM }.map { it.text })
            .filter { it.isNotBlank() }
            .joinToString("\n\n")

        val merged = mutableListOf<Pair<String, StringBuilder>>()
        request.messages.filter { it.role != ChatRole.SYSTEM }.forEach { message ->
            val role = if (message.role == ChatRole.USER) "user" else "assistant"
            val last = merged.lastOrNull()
            if (last != null && last.first == role) {
                last.second.append("\n\n").append(message.text)
            } else {
                merged.add(role to StringBuilder(message.text))
            }
        }
        val messages = JSONArray()
        merged.forEach { (role, text) ->
            messages.put(JSONObject().put("role", role).put("content", text.toString()))
        }

        val body = JSONObject()
            .put("model", config.model)
            .put("max_tokens", request.maxTokens ?: DEFAULT_MAX_TOKENS)
            .put("messages", messages)
            .put("stream", stream)
        if (systemText.isNotEmpty()) body.put("system", systemText)
        request.temperature?.let { body.put("temperature", it) }

        val headers = linkedMapOf(
            "x-api-key" to config.apiKey,
            "Authorization" to "Bearer ${config.apiKey}",
            "anthropic-version" to "2023-06-01",
            "Accept" to if (stream) "text/event-stream" else "application/json",
        )
        headers.putAll(config.extraHeaders)
        return buildPost(endpoint(config.baseUrl), body, headers)
    }

    override fun parseChunk(eventName: String?, data: String): List<ChatEvent> {
        val trimmed = data.trim()
        if (trimmed.isEmpty() || trimmed == "[DONE]") return emptyList()
        val json = parseJson(trimmed)
        val type = json.textOrNull("type") ?: eventName.orEmpty()
        if (type == "error") {
            throw errorFromJson(json) ?: com.island.phone.net.ApiException(
                com.island.phone.net.ApiErrorKind.SERVER,
                null,
                "服务返回了错误",
            )
        }
        errorFromJson(json)?.let { throw it }
        return when (type) {
            "content_block_delta" -> {
                val delta = json.optJSONObject("delta")
                if (delta?.textOrNull("type") == "text_delta") {
                    delta.textOrNull("text")?.let { listOf(ChatEvent.Delta(it)) } ?: emptyList()
                } else {
                    emptyList()
                }
            }
            "message_delta" -> {
                json.optJSONObject("delta")?.textOrNull("stop_reason")
                    ?.let { listOf(ChatEvent.Done(it)) } ?: emptyList()
            }
            "message_stop" -> listOf(ChatEvent.Done(null))
            else -> emptyList()
        }
    }

    override fun parseFull(body: String): List<ChatEvent> {
        val json = parseJson(body)
        if (json.textOrNull("type") == "error") {
            throw errorFromJson(json) ?: com.island.phone.net.ApiException(
                com.island.phone.net.ApiErrorKind.SERVER,
                null,
                "服务返回了错误",
            )
        }
        errorFromJson(json)?.let { throw it }
        val text = StringBuilder()
        val blocks = json.optJSONArray("content")
        if (blocks != null) {
            for (i in 0 until blocks.length()) {
                val block = blocks.optJSONObject(i) ?: continue
                if (block.textOrNull("type") == "text") {
                    block.textOrNull("text")?.let { text.append(it) }
                }
            }
        }
        val events = mutableListOf<ChatEvent>()
        if (text.isNotEmpty()) events.add(ChatEvent.Delta(text.toString()))
        events.add(ChatEvent.Done(json.textOrNull("stop_reason")))
        return events
    }

    private fun endpoint(base: String): String {
        val trimmed = trimBase(base)
        return when {
            trimmed.endsWith("/messages") -> trimmed
            trimmed.endsWith("/v1") -> "$trimmed/messages"
            else -> "$trimmed/v1/messages"
        }
    }

    private companion object {
        const val DEFAULT_MAX_TOKENS = 4096
    }
}
