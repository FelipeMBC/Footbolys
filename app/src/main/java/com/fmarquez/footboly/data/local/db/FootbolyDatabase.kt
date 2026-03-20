package com.fmarquez.footboly.data.local.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.room.Room
import com.fmarquez.footboly.data.local.dao.MatchDao
import com.fmarquez.footboly.data.local.dao.TeamDao
import com.fmarquez.footboly.data.local.entity.MatchEntity
import com.fmarquez.footboly.data.local.entity.MatchEventEntity
import com.fmarquez.footboly.data.local.entity.MatchPlayerEntity
import com.fmarquez.footboly.data.local.entity.PlayerEntity
import com.fmarquez.footboly.data.local.entity.TeamEntity

@Database(
    entities = [
        TeamEntity::class,
        PlayerEntity::class,
        MatchEntity::class,
        MatchPlayerEntity::class,
        MatchEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FootbolyDatabase : RoomDatabase() {
    abstract fun teamDao(): TeamDao
    abstract fun matchDao(): MatchDao

    companion object {
        @Volatile
        private var INSTANCE: FootbolyDatabase? = null

        fun getDatabase(context: Context): FootbolyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FootbolyDatabase::class.java,
                    "footboly_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}