package com.island.phone.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity): Long

    @Insert
    suspend fun insertAll(messages: List<MessageEntity>): List<Long>

    @Update
    suspend fun update(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)

    @Query("DELETE FROM messages WHERE charId = :charId")
    suspend fun deleteByChar(charId: Long)

    @Query("DELETE FROM messages")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId")
    suspend fun countBySession(sessionId: Long): Int

    @Query("SELECT COUNT(*) FROM messages WHERE charId = :charId")
    suspend fun countByChar(charId: Long): Int

    @Query(
        "SELECT * FROM messages WHERE sessionId = :sessionId " +
            "ORDER BY createdAt DESC, id DESC LIMIT :limit",
    )
    suspend fun latestBySession(sessionId: Long, limit: Int): List<MessageEntity>

    @Query(
        "SELECT * FROM messages WHERE sessionId = :sessionId " +
            "AND (createdAt < :beforeTime OR (createdAt = :beforeTime AND id < :beforeId)) " +
            "ORDER BY createdAt DESC, id DESC LIMIT :limit",
    )
    suspend fun olderBySession(
        sessionId: Long,
        beforeTime: Long,
        beforeId: Long,
        limit: Int,
    ): List<MessageEntity>

    @Query(
        "SELECT * FROM messages WHERE charId = :charId " +
            "ORDER BY createdAt DESC, id DESC LIMIT :limit",
    )
    suspend fun latestByChar(charId: Long, limit: Int): List<MessageEntity>

    @Query(
        "SELECT * FROM messages WHERE charId = :charId " +
            "AND (createdAt < :beforeTime OR (createdAt = :beforeTime AND id < :beforeId)) " +
            "ORDER BY createdAt DESC, id DESC LIMIT :limit",
    )
    suspend fun olderByChar(
        charId: Long,
        beforeTime: Long,
        beforeId: Long,
        limit: Int,
    ): List<MessageEntity>
}
