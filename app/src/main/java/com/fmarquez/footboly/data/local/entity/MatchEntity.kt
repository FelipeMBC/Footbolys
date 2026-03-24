package com.fmarquez.footboly.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "matches",
    indices = [Index("teamId"), Index("isFinished")]
)
data class MatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val teamId: Int,
    val teamNameSnapshot: String,
    val shirtColorHex: String = "#1E6B45",
    val isStarted: Boolean = false,
    val isFinished: Boolean = false,
    val totalSeconds: Int = 60,
    val remainingSeconds: Int = 60,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val finishedAtMillis: Long? = null,
    val finishedAtLabel: String = ""
)