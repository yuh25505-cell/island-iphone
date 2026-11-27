package com.island.phone.service

import android.annotation.SuppressLint
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.island.phone.MainActivity
import com.island.phone.R

object ReplyNotifier {
    const val RUNNING_ID = 1001
    private const val CHANNEL_RUNNING = "reply_running"
    private const val CHANNEL_DONE = "reply_done"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RUNNING,
                context.getString(R.string.reply_channel_running),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DONE,
                context.getString(R.string.reply_channel_done),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    fun buildRunning(context: Context, count: Int = 1): Notification =
        NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_stat_island)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(
                if (count > 1) {
                    context.getString(R.string.reply_running_count, count)
                } else {
                    context.getString(R.string.reply_running_text)
                },
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(openAppIntent(context))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    fun notifyDone(context: Context, taskId: String, title: String, text: String) {
        val preview = text.replace(Regex("\\s+"), " ").trim().take(100)
        post(context, taskId, title, preview)
    }

    fun notifyFailed(context: Context, taskId: String, title: String) {
        post(context, taskId, title, context.getString(R.string.reply_failed_text))
    }

    fun cancel(context: Context, taskId: String) {
        NotificationManagerCompat.from(context).cancel(taskId.hashCode())
    }

    @SuppressLint("MissingPermission")
    private fun post(context: Context, taskId: String, title: String, text: String) {
        ensureChannels(context)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val compat = NotificationManagerCompat.from(context)
        if (!compat.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_island)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        compat.notify(taskId.hashCode(), notification)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
