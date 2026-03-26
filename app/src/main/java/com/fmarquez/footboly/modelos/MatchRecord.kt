package com.fmarquez.footboly.modelos

data class MatchPlayerTime(
    val playerId: Int,
    val accumulatedSeconds: Int = 0,
    val isCurrentlyPlaying: Boolean = false,
    val lastEntrySecond: Int? = null
)

data class MatchRecord(
    val id: Int,
    val teamId: Int,
    val teamName: String,
    val shirtColorHex: String = "#1E6B45",
    val rivalName: String = "",
    val matchDateLabel: String = "",
    val starters: MutableList<Player>,
    val substitutes: MutableList<Player>,
    val expelledPlayers: MutableList<Player> = mutableListOf(),
    val injuredPlayers: MutableList<Player> = mutableListOf(),
    val statsByPlayerId: MutableMap<Int, PlayerStats> = mutableMapOf(),
    val events: MutableList<MatchEvent>,
    val playerTimes: MutableMap<Int, MatchPlayerTime> = mutableMapOf(),
    val opponentGoals: Int = 0,
    val opponentGoalChances: Int = 0,
    val isStarted: Boolean = false,
    val isFinished: Boolean = false,
    val totalSeconds: Int = 60,
    val remainingSeconds: Int = 60,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val finishedAtMillis: Long? = null,
    val finishedAtLabel: String = ""
)