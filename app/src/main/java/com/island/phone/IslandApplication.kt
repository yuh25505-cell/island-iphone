package com.island.phone

import android.app.Application
import com.island.phone.data.db.IslandDatabase
import com.island.phone.data.repo.MessageRepository
import com.island.phone.net.ChatClient
import com.island.phone.system.backup.BackupManager

class IslandApplication : Application() {
    val database: IslandDatabase by lazy { IslandDatabase.build(this) }
    val messageRepository: MessageRepository by lazy { MessageRepository(database.messageDao()) }
    val backupManager: BackupManager by lazy { BackupManager(this, database) }
    val chatClient: ChatClient by lazy { ChatClient() }
}
