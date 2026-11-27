package com.island.phone.net

enum class ApiFormat { OPENAI, CLAUDE, GEMINI }

enum class ChatRole { SYSTEM, USER, ASSISTANT }

data class ChatMessage(val role: ChatRole, val text: String)

data class ChatRequest(
    val messages: List<ChatMessage>,
    val system: String? = null,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
)

data class ApiConfig(
    val format: ApiFormat,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val stream: Boolean = true,
    val extraHeaders: Map<String, String> = emptyMap(),
)

sealed interface ChatEvent {
    data class Delta(val text: String) : ChatEvent
    data class Done(val finishReason: String?) : ChatEvent
    data class Retrying(val attempt: Int) : ChatEvent
}

enum class ApiErrorKind(val retryable: Boolean) {
    NETWORK(true),
    TIMEOUT(true),
    RATE_LIMIT(true),
    SERVER(true),
    AUTH(false),
    BAD_URL(false),
    BAD_REQUEST(false),
    PARSE(false),
    EMPTY(false),
}

class ApiException(
    val kind: ApiErrorKind,
    val httpCode: Int?,
    message: String,
    val retryAfterMs: Long? = null,
) : Exception(message)
