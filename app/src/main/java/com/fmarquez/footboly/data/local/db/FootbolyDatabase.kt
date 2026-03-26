package com.fmarquez.footboly.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fmarquez.footboly.data.local.dao.MatchDao
import com.fmarquez.footboly.data.local.dao.TeamDao
import com.fmarquez.footboly.data.local.entity.MatchEntity
import com.fmarquez.footboly.data.local.entity.MatchEventEntity
import com.fmarquez.footboly.data.local.entity.MatchPlayerEntity
import com.fmarquez.footboly.data.local.entity.MatchPlayerTimeEntity
import com.fmarquez.footboly.data.local.entity.PlayerEntity
import com.fmarquez.footboly.data.local.entity.TeamEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE teams ADD COLUMN logoUri TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE teams ADD COLUMN shirtColorHex TEXT NOT NULL DEFAULT '#1E6B45'")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE matches ADD COLUMN shirtColorHex TEXT NOT NULL DEFAULT '#1E6B45'")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE matches ADD COLUMN rivalName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE matches ADD COLUMN matchDateLabel TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS match_player_times (
                matchId INTEGER NOT NULL,
                playerId INTEGER NOT NULL,
                accumulatedSeconds INTEGER NOT NULL DEFAULT 0,
                isCurrentlyPlaying INTEGER NOT NULL DEFAULT 0,
                lastEntrySecond INTEGER,
                PRIMARY KEY(matchId, playerId),
                FOREIGN KEY(matchId) REFERENCES matches(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_match_player_times_matchId ON match_player_times(matchId)"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_match_player_times_playerId ON match_player_times(playerId)"
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE matches ADD COLUMN opponentGoals INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE matches ADD COLUMN opponentGoalChances INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [
        TeamEntity::class,
        PlayerEntity::class,
        MatchEntity::class,
        MatchPlayerEntity::class,
        MatchEventEntity::class,
        MatchPlayerTimeEntity::class
    ],
    version = 7,
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
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}