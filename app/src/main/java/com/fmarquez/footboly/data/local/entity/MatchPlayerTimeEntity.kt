package com.fmarquez.footboly.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "match_player_times",
    primaryKeys = ["matchId", "playerId"],
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("matchId"), Index("playerId")]
)
data class MatchPlayerTimeEntity(
    val matchId: Int,
    val playerId: Int,
    val accumulatedSeconds: Int = 0,
    val isCurrentlyPlaying: Boolean = false,
    val lastEntrySecond: Int? = null
)