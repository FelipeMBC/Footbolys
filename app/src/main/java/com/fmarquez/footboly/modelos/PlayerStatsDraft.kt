package com.fmarquez.footboly.modelos

data class PlayerStatsDraft(
    val playerId: Int,
    val matchId: Int,
    var gol: Int = 0,
    var asistencia: Int = 0,
    var amarilla: Int = 0,
    var roja: Int = 0,
    var disparosAlArco: Int = 0,
    var ocasionesDeGol: Int = 0,
    var pelotasPerdidas: Int = 0,
    var pelotasRecuperadas: Int = 0,
    var centrosBuenos: Int = 0,
    var centrosMalos: Int = 0,
    var faltaAFavor: Int = 0,
    var faltaEnContra: Int = 0,
    var cornerAFavor: Int = 0,
    var cornerEnContra: Int = 0,
    var tiroLibreAFavor: Int = 0,
    var tiroLibreEnContra: Int = 0,
    var tiroLibreLateralAFavor: Int = 0,
    var tiroLibreLateralEnContra: Int = 0
)