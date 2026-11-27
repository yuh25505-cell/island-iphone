package com.island.phone.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object SenderType {
    const val USER = 0
    const val CHAR = 1
    const val SYSTEM = 2
}

object MessageKind {
    const val TEXT = 0
    const val IMAGE = 1
    const val STICKER = 2
    const val VOICE = 3
    const val TRANSFER = 4
}

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["charId", "createdAt", "id"]),
        Index(value = ["sessionId", "createdAt", "id"]),
    ],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val charId: Long,
    val sessionId: Long,
    val senderType: Int,
    val kind: Int,
    val content: String,
    val filePath: String? = null,
    val createdAt: Long,
)
