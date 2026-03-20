package com.fmarquez.footboly.modelos

data class MatchEvent(
    val minute: Int,
    val type: String,
    val playerId: Int? = null,
    val playerName: String,
    val detail: String = "",
    val timestampLabel: String = ""

)