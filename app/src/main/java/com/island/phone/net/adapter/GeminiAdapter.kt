package com.island.phone.net.adapter

import com.island.phone.net.ApiAdapter
import com.island.phone.net.ApiConfig
import com.island.phone.net.ApiErrorKind
import com.island.phone.net.ApiException
import com.island.phone.net.ChatEvent
import com.island.phone.net.ChatRequest
import com.island.phone.net.ChatRole
import com.island.phone.net.buildPost
import com.island.phone.net.errorFromJson
import com.island.phone.net.parseJson
import com.island.phone.net.textOrNull
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

internal class GeminiAdapter : ApiAdapter {

    override fun buildRequest(config: ApiConfig, request: ChatRequest, stream: Boolean): Request {
        val systemText = (listOfNotNull(request.system) +
            request.messages.filter { it.role == ChatRole.SYSTEM }.map { it.text })
            .filter { it.isNotBlank() }
            .joinToString("\n\n")

        val contents = JSONArray()
        request.messages.filter { it.role != ChatRole.SYSTEM }.forEach { message ->
            val role = if (message.role == ChatRole.USER) "user" else "model"
            contents.put(
                JSONObject()
                    .put("role", role)
                    .put("parts", JSONArray().put(JSONObject().put("text", message.text))),
            )
        }

        val body = JSONObject().put("contents", contents)
        if (systemText.isNotEmpty()) {
            body.put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemText))),
            )
        }
        if (request.temperature != null || request.maxTokens != null) {
            val generation = JSONObject()
            request.temperature?.let { generation.put("temperature", it) }
            request.maxTokens?.let { generation.put("maxOutputTokens", it) }
            body.put("generationConfig", generation)
        }

        val headers = linkedMapOf("x-goog-api-key" to config.apiKey)
        headers.putAll(config.extraHeaders)
        return buildPost(config.baseUrl.trim(), body, headers)
    }

    override fun parseChunk(eventName: String?, data: String): List<ChatEvent> {
        val trimmed = data.trim()
        if (trimmed.isEmpty() || trimmed == "[DONE]") return emptyList()
        return parseResponse(parseJson(trimmed))
    }

    override fun parseFull(body: String): List<ChatEvent> = parseResponse(parseJson(body))

    private fun parseResponse(json: JSONObject): List<ChatEvent> {
        errorFromJson(json)?.let { throw it }
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        if (candidate == null) {
            val blocked = json.optJSONObject("promptFeedback")?.textOrNull("blockReason")
            if (blocked != null) {
                throw ApiException(ApiErrorKind.EMPTY, null, "内容被拦截：$blocked")
            }
            return emptyList()
        }
        val text = StringBuilder()
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
        if (parts != null) {
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i) ?: continue
                if (part.optBoolean("thought", false)) continue
                part.textOrNull("text")?.let { text.append(it) }
            }
        }
        val events = mutableListOf<ChatEvent>()
        if (text.isNotEmpty()) events.add(ChatEvent.Delta(text.toString()))
        candidate.textOrNull("finishReason")?.let { events.add(ChatEvent.Done(it)) }
        return events
    }
}
