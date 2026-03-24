package com.fmarquez.footboly.modelos

data class Team(
    val id: Int,
    val name: String,
    val logoEmoji: String,
    val logoUri: String? = null,
    val shirtColorHex: String = "#1E6B45",
    val players: List<Player>
)