package com.fmarquez.footboly.modelos

data class MatchRecord(
    val id: Int,
    val teamName: String,
    val starters: MutableList<Player>,
    val substitutes: MutableList<Player>,
    val statsByPlayerId: MutableMap<Int, PlayerStats>,
    val events: MutableList<MatchEvent>,
    val isStarted: Boolean = false,
    val isFinished: Boolean = false,
    val totalSeconds: Int = 60,
    val remainingSeconds: Int = 60,
    val finishedAtLabel: String = ""
)