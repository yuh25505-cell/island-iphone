package com.island.phone.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

const val DB_VERSION = 1
const val DB_NAME = "island.db"

@Database(
    entities = [MessageEntity::class],
    version = DB_VERSION,
    exportSchema = true,
)
abstract class IslandDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao

    companion object {
        fun build(context: Context): IslandDatabase =
            Room.databaseBuilder(context.applicationContext, IslandDatabase::class.java, DB_NAME)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}
