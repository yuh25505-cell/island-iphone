package com.island.phone.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.island.phone.IslandApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReplyService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watcher: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastStartId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        val manager = (application as IslandApplication).replyTaskManager
        ReplyNotifier.ensureChannels(this)
        ServiceCompat.startForeground(
            this,
            ReplyNotifier.RUNNING_ID,
            ReplyNotifier.buildRunning(this, manager.activeCount.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        acquireWakeLock()
        if (watcher?.isActive != true) {
            watcher = scope.launch {
                val renewer = launch {
                    while (true) {
                        delay(WAKE_LOCK_RENEW_MS)
                        acquireWakeLock()
                    }
                }
                var shown = manager.activeCount.value
                manager.activeCount.first { count ->
                    if (count > 0 && count != shown) {
                        shown = count
                        getSystemService(NotificationManager::class.java)
                            .notify(ReplyNotifier.RUNNING_ID, ReplyNotifier.buildRunning(this@ReplyService, count))
                    }
                    count == 0
                }
                renewer.cancel()
                releaseWakeLock()
                ServiceCompat.stopForeground(this@ReplyService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf(lastStartId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        watcher?.cancel()
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        val lock = wakeLock ?: (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "island:reply")
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        lock.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private companion object {
        const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L
        const val WAKE_LOCK_RENEW_MS = 5 * 60 * 1000L
    }
}
