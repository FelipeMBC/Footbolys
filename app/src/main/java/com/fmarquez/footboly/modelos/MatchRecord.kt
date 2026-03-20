package com.fmarquez.footboly.modelos

data class MatchRecord(
    val id: Int,
    val teamId: Int,
    val teamName: String,
    val starters: MutableList<Player>,
    val substitutes: MutableList<Player>,
    val statsByPlayerId: MutableMap<Int, PlayerStats> = mutableMapOf(),
    val events: MutableList<MatchEvent>,
    val isStarted: Boolean = false,
    val isFinished: Boolean = false,
    val totalSeconds: Int = 60,
    val remainingSeconds: Int = 60,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val finishedAtMillis: Long? = null,
    val finishedAtLabel: String = ""
)