package com.island.phone.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ChatClient(
    private val client: OkHttpClient = HttpClients.shared,
    private val defaultRetries: Int = 2,
) {

    fun chat(
        config: ApiConfig,
        request: ChatRequest,
        maxRetries: Int = defaultRetries,
    ): Flow<ChatEvent> = flow {
        var attempt = 0
        while (true) {
            try {
                attemptOnce(config, request).collect { emit(it) }
                return@flow
            } catch (e: ApiException) {
                if (!e.kind.retryable || attempt >= maxRetries) throw e
                attempt++
                emit(ChatEvent.Retrying(attempt))
                delay(e.retryAfterMs ?: (1000L shl (attempt - 1)))
            }
        }
    }

    suspend fun testConnection(config: ApiConfig): ApiException? = try {
        chat(
            config = config,
            request = ChatRequest(messages = listOf(ChatMessage(ChatRole.USER, "hi"))),
            maxRetries = 0,
        ).collect { }
        null
    } catch (e: ApiException) {
        e
    }

    private fun attemptOnce(config: ApiConfig, request: ChatRequest): Flow<ChatEvent> = flow {
        val adapter = adapterFor(config.format)
        val httpRequest = try {
            adapter.buildRequest(config, request, config.stream)
        } catch (e: IllegalArgumentException) {
            throw ApiException(ApiErrorKind.BAD_URL, null, "接口地址不正确")
        }

        var total = 0
        var finished = false

        suspend fun handle(event: ChatEvent) {
            when (event) {
                is ChatEvent.Delta -> {
                    total += event.text.length
                    emit(event)
                }
                is ChatEvent.Done -> {
                    finished = true
                    emit(event)
                }
                is ChatEvent.Retrying -> Unit
            }
        }

        val call = client.newCall(httpRequest)
        val handle = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        try {
            val response = try {
                call.execute()
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                throw mapIoError(e)
            }
            response.use { resp ->
                if (!resp.isSuccessful) throw httpError(resp)
                val body = resp.body ?: throw ApiException(ApiErrorKind.PARSE, null, "服务没有返回内容")
                val contentType = resp.header("Content-Type").orEmpty()
                val plainJson = contentType.contains("json", ignoreCase = true) &&
                    !contentType.contains("event-stream", ignoreCase = true)

                try {
                    if (config.stream && !plainJson) {
                        val reader = SseReader(body.source())
                        while (!finished) {
                            val sse = reader.next() ?: break
                            for (event in adapter.parseChunk(sse.name, sse.data)) {
                                handle(event)
                                if (finished) break
                            }
                        }
                    } else {
                        for (event in adapter.parseFull(body.string())) {
                            handle(event)
                            if (finished) break
                        }
                    }
                } catch (e: IOException) {
                    currentCoroutineContext().ensureActive()
                    throw mapIoError(e)
                }

                if (total == 0) throw ApiException(ApiErrorKind.EMPTY, null, "模型没有返回任何内容")
                if (!finished) emit(ChatEvent.Done(null))
            }
        } finally {
            handle?.dispose()
        }
    }.flowOn(Dispatchers.IO)

    private fun httpError(response: Response): ApiException {
        val raw = runCatching { response.body?.string().orEmpty().take(2000) }.getOrDefault("")
        val message = extractErrorMessage(raw) ?: raw.ifBlank { response.message }
        val retryAfter = response.header("Retry-After")?.toLongOrNull()?.times(1000)?.coerceAtMost(15_000L)
        return ApiException(kindForStatus(response.code), response.code, message, retryAfter)
    }

    private fun mapIoError(e: IOException): ApiException = when (e) {
        is SocketTimeoutException -> ApiException(ApiErrorKind.TIMEOUT, null, "连接超时")
        is UnknownHostException -> ApiException(ApiErrorKind.NETWORK, null, "找不到服务器，请检查网络或接口地址")
        else -> ApiException(ApiErrorKind.NETWORK, null, e.message ?: "网络中断")
    }
}
