package com.island.phone.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.island.phone.net.ApiConfig
import com.island.phone.net.ApiErrorKind
import com.island.phone.net.ApiException
import com.island.phone.net.ChatClient
import com.island.phone.net.ChatEvent
import com.island.phone.net.ChatRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

sealed interface ReplyState {
    data class Running(val text: String, val retryAttempt: Int = 0) : ReplyState
    data class Finished(val text: String, val finishReason: String?) : ReplyState
    data class Failed(val error: ApiException) : ReplyState
}

class ReplyTaskManager(
    private val context: Context,
    private val chatClient: ChatClient,
    private val foreground: AppForeground,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val jobs = ConcurrentHashMap<String, Job>()

    private val _states = MutableStateFlow<Map<String, ReplyState>>(emptyMap())
    val states: StateFlow<Map<String, ReplyState>> = _states.asStateFlow()

    private val _activeCount = MutableStateFlow(0)
    val activeCount: StateFlow<Int> = _activeCount.asStateFlow()

    fun start(
        taskId: String,
        title: String,
        config: ApiConfig,
        request: ChatRequest,
        onFinished: suspend (text: String, finishReason: String?) -> Unit = { _, _ -> },
    ) {
        if (jobs.containsKey(taskId)) return
        setState(taskId, ReplyState.Running(""))
        val job = scope.launch(start = CoroutineStart.LAZY) {
            runTask(taskId, title, config, request, onFinished)
        }
        jobs[taskId] = job
        _activeCount.value = jobs.size
        startKeepAlive()
        job.start()
    }

    fun cancel(taskId: String) {
        jobs[taskId]?.cancel()
    }

    fun consume(taskId: String) {
        val current = _states.value[taskId]
        if (current != null && current !is ReplyState.Running) removeState(taskId)
    }

    private suspend fun runTask(
        taskId: String,
        title: String,
        config: ApiConfig,
        request: ChatRequest,
        onFinished: suspend (String, String?) -> Unit,
    ) {
        val text = StringBuilder()
        var reason: String? = null
        var retryAttempt = 0
        try {
            chatClient.chat(config, request).collect { event ->
                when (event) {
                    is ChatEvent.Delta -> {
                        text.append(event.text)
                        setState(taskId, ReplyState.Running(text.toString(), retryAttempt))
                    }
                    is ChatEvent.Retrying -> {
                        text.setLength(0)
                        retryAttempt = event.attempt
                        setState(taskId, ReplyState.Running("", retryAttempt))
                    }
                    is ChatEvent.Done -> reason = event.finishReason
                }
            }
            val finalText = text.toString()
            try {
                onFinished(finalText, reason)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 保存失败不影响回复本身的显示
            }
            setState(taskId, ReplyState.Finished(finalText, reason))
            if (!foreground.isForeground) ReplyNotifier.notifyDone(context, taskId, title, finalText)
        } catch (e: CancellationException) {
            removeState(taskId)
            throw e
        } catch (e: ApiException) {
            setState(taskId, ReplyState.Failed(e))
            if (!foreground.isForeground) ReplyNotifier.notifyFailed(context, taskId, title)
        } catch (e: Exception) {
            setState(
                taskId,
                ReplyState.Failed(ApiException(ApiErrorKind.PARSE, null, e.message ?: "未知错误")),
            )
            if (!foreground.isForeground) ReplyNotifier.notifyFailed(context, taskId, title)
        } finally {
            jobs.remove(taskId, coroutineContext[Job])
            _activeCount.value = jobs.size
        }
    }

    private fun startKeepAlive() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, ReplyService::class.java))
        } catch (e: Exception) {
            // 系统不允许时仍继续回复，只是没有保活保护
        }
    }

    private fun setState(taskId: String, state: ReplyState) {
        _states.value = _states.value + (taskId to state)
    }

    private fun removeState(taskId: String) {
        _states.value = _states.value - taskId
    }
}
