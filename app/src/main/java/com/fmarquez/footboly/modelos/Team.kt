package com.fmarquez.footboly.modelos

data class Team(
    val id: Int,
    val name: String,
    val logoEmoji: String,
    val logoUri: String? = null,
    val players: List<Player>
)