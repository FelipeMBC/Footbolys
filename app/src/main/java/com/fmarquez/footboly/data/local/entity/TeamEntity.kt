package com.fmarquez.footboly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val logoEmoji: String,
    val logoUri: String? = null,
    val shirtColorHex: String = "#1E6B45"
)