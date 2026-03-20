package com.fmarquez.footboly.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "match_players",
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
data class MatchPlayerEntity(
    val matchId: Int,
    val playerId: Int,
    val role: String,
    val playerNameSnapshot: String,
    val playerNumberSnapshot: Int
)