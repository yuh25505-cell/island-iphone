package com.island.phone

import android.app.Application
import com.island.phone.data.db.IslandDatabase
import com.island.phone.data.repo.MessageRepository
import com.island.phone.net.ChatClient
import com.island.phone.service.AppForeground
import com.island.phone.service.ReplyTaskManager
import com.island.phone.system.backup.BackupManager

class IslandApplication : Application() {
    lateinit var appForeground: AppForeground
        private set

    val database: IslandDatabase by lazy { IslandDatabase.build(this) }
    val messageRepository: MessageRepository by lazy { MessageRepository(database.messageDao()) }
    val backupManager: BackupManager by lazy { BackupManager(this, database) }
    val chatClient: ChatClient by lazy { ChatClient() }
    val replyTaskManager: ReplyTaskManager by lazy { ReplyTaskManager(this, chatClient, appForeground) }

    override fun onCreate() {
        super.onCreate()
        appForeground = AppForeground(this)
    }
}
