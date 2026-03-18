package com.fmarquez.footboly.modelos

data class PlayerStats(
    var yellowCards: Int = 0,
    var redCards: Int = 0,
    var goals: Int = 0,
    var assists: Int = 0,
    var corners: Int = 0,
    var freeKicks: Int = 0,
    var recoveries: Int = 0
)