package com.island.phone.data.repo

import com.island.phone.data.db.MessageDao
import com.island.phone.data.db.MessageEntity

const val MESSAGE_PAGE_SIZE = 40

class MessageRepository(private val dao: MessageDao) {

    suspend fun add(message: MessageEntity): Long = dao.insert(message)

    suspend fun addAll(messages: List<MessageEntity>): List<Long> = dao.insertAll(messages)

    suspend fun edit(message: MessageEntity) = dao.update(message)

    suspend fun remove(id: Long) = dao.deleteById(id)

    suspend fun removeSession(sessionId: Long) = dao.deleteBySession(sessionId)

    suspend fun removeChar(charId: Long) = dao.deleteByChar(charId)

    suspend fun latestInSession(sessionId: Long, pageSize: Int = MESSAGE_PAGE_SIZE): List<MessageEntity> =
        dao.latestBySession(sessionId, pageSize)

    suspend fun olderInSession(
        sessionId: Long,
        oldest: MessageEntity,
        pageSize: Int = MESSAGE_PAGE_SIZE,
    ): List<MessageEntity> =
        dao.olderBySession(sessionId, oldest.createdAt, oldest.id, pageSize)

    suspend fun latestOfChar(charId: Long, pageSize: Int = MESSAGE_PAGE_SIZE): List<MessageEntity> =
        dao.latestByChar(charId, pageSize)

    suspend fun olderOfChar(
        charId: Long,
        oldest: MessageEntity,
        pageSize: Int = MESSAGE_PAGE_SIZE,
    ): List<MessageEntity> =
        dao.olderByChar(charId, oldest.createdAt, oldest.id, pageSize)

    suspend fun countInSession(sessionId: Long): Int = dao.countBySession(sessionId)

    suspend fun countOfChar(charId: Long): Int = dao.countByChar(charId)
}
