package com.island.phone.net.adapter

import com.island.phone.net.ApiAdapter
import com.island.phone.net.ApiConfig
import com.island.phone.net.ChatEvent
import com.island.phone.net.ChatRequest
import com.island.phone.net.buildPost
import com.island.phone.net.errorFromJson
import com.island.phone.net.parseJson
import com.island.phone.net.roleName
import com.island.phone.net.textOrNull
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

internal class OpenAiAdapter : ApiAdapter {

    override fun buildRequest(config: ApiConfig, request: ChatRequest, stream: Boolean): Request {
        val messages = JSONArray()
        request.system?.takeIf { it.isNotBlank() }?.let {
            messages.put(JSONObject().put("role", "system").put("content", it))
        }
        request.messages.forEach {
            messages.put(JSONObject().put("role", roleName(it.role)).put("content", it.text))
        }
        val body = JSONObject()
            .put("model", config.model)
            .put("messages", messages)
            .put("stream", stream)
        request.temperature?.let { body.put("temperature", it) }
        request.maxTokens?.let { body.put("max_tokens", it) }

        val headers = linkedMapOf(
            "Authorization" to "Bearer ${config.apiKey}",
            "Accept" to if (stream) "text/event-stream" else "application/json",
        )
        headers.putAll(config.extraHeaders)
        return buildPost(config.baseUrl.trim(), body, headers)
    }

    override fun parseChunk(eventName: String?, data: String): List<ChatEvent> {
        val trimmed = data.trim()
        if (trimmed == "[DONE]") return listOf(ChatEvent.Done(null))
        if (trimmed.isEmpty()) return emptyList()
        val json = parseJson(trimmed)
        errorFromJson(json)?.let { throw it }
        val choice = json.optJSONArray("choices")?.optJSONObject(0) ?: return emptyList()
        val events = mutableListOf<ChatEvent>()
        choice.optJSONObject("delta")?.textOrNull("content")?.let { events.add(ChatEvent.Delta(it)) }
        choice.textOrNull("finish_reason")?.let { events.add(ChatEvent.Done(it)) }
        return events
    }

    override fun parseFull(body: String): List<ChatEvent> {
        val json = parseJson(body)
        errorFromJson(json)?.let { throw it }
        val choice = json.optJSONArray("choices")?.optJSONObject(0) ?: return emptyList()
        val events = mutableListOf<ChatEvent>()
        choice.optJSONObject("message")?.textOrNull("content")?.let { events.add(ChatEvent.Delta(it)) }
        events.add(ChatEvent.Done(choice.textOrNull("finish_reason")))
        return events
    }
}
