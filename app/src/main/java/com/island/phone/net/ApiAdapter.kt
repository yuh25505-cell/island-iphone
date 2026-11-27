package com.island.phone.net

import com.island.phone.net.adapter.ClaudeAdapter
import com.island.phone.net.adapter.GeminiAdapter
import com.island.phone.net.adapter.OpenAiAdapter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject

internal interface ApiAdapter {
    fun buildRequest(config: ApiConfig, request: ChatRequest, stream: Boolean): Request
    fun parseChunk(eventName: String?, data: String): List<ChatEvent>
    fun parseFull(body: String): List<ChatEvent>
}

internal fun adapterFor(format: ApiFormat): ApiAdapter = when (format) {
    ApiFormat.OPENAI -> OpenAiAdapter()
    ApiFormat.CLAUDE -> ClaudeAdapter()
    ApiFormat.GEMINI -> GeminiAdapter()
}

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

internal fun trimBase(base: String): String = base.trim().trimEnd('/')

internal fun roleName(role: ChatRole): String = when (role) {
    ChatRole.SYSTEM -> "system"
    ChatRole.USER -> "user"
    ChatRole.ASSISTANT -> "assistant"
}

internal fun buildPost(url: String, body: JSONObject, headers: Map<String, String>): Request {
    val builder = Request.Builder()
        .url(url)
        .post(body.toString().toRequestBody(JSON_MEDIA))
    headers.forEach { (name, value) -> builder.header(name, value) }
    return builder.build()
}

internal fun JSONObject.textOrNull(key: String): String? =
    if (isNull(key)) null else optString(key, "").ifEmpty { null }

internal fun parseJson(text: String): JSONObject = try {
    JSONObject(text.trim())
} catch (e: JSONException) {
    throw ApiException(ApiErrorKind.PARSE, null, "服务返回的内容格式不对")
}

internal fun kindForStatus(code: Int): ApiErrorKind = when {
    code == 401 || code == 403 -> ApiErrorKind.AUTH
    code == 404 || code == 405 -> ApiErrorKind.BAD_URL
    code == 408 -> ApiErrorKind.TIMEOUT
    code == 429 -> ApiErrorKind.RATE_LIMIT
    code in 500..599 -> ApiErrorKind.SERVER
    else -> ApiErrorKind.BAD_REQUEST
}

internal fun extractErrorMessage(raw: String): String? = runCatching {
    val json = JSONObject(raw.trim())
    json.optJSONObject("error")?.textOrNull("message")
        ?: json.textOrNull("error")
        ?: json.textOrNull("message")
        ?: json.textOrNull("detail")
}.getOrNull()

internal fun errorFromJson(json: JSONObject): ApiException? {
    if (!json.has("error") || json.isNull("error")) return null
    val err = json.optJSONObject("error")
    val message = extractErrorMessage(json.toString()) ?: "服务返回了错误"
    val code = err?.optInt("code", -1) ?: -1
    val type = (err?.textOrNull("type") ?: err?.textOrNull("status") ?: "").lowercase()
    val kind = when {
        code in 100..599 -> kindForStatus(code)
        type.contains("auth") -> ApiErrorKind.AUTH
        type.contains("rate") -> ApiErrorKind.RATE_LIMIT
        type.contains("overloaded") || type.contains("server") -> ApiErrorKind.SERVER
        else -> ApiErrorKind.BAD_REQUEST
    }
    return ApiException(kind, code.takeIf { it in 100..599 }, message)
}
