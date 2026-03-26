package com.fmarquez.footboly.modelos

data class PlayerStatsDraft(
    val playerId: Int,
    val matchId: Int,

    // Bloque 1
    var golFavor: Int = 0,
    var golContra: Int = 0,
    var tiroAlArcoPositivo: Int = 0,
    var tiroAlArcoNegativo: Int = 0,
    var participacionGolFavor: Int = 0,
    var participacionGolContra: Int = 0,
    var remate12Positivo: Int = 0,
    var remate12Negativo: Int = 0,

    // Bloque 2
    var balonRecogidoFavor: Int = 0,
    var balonRecogidoContra: Int = 0,
    var pasesBuenos: Int = 0,
    var pasesMalos: Int = 0,
    var centrosPositivos: Int = 0,
    var centrosNegativos: Int = 0,
    var rechazosPositivos: Int = 0,
    var rechazosNegativos: Int = 0,

    // Bloque 3
    var faltaFavor: Int = 0,
    var faltaContra: Int = 0,
    var cornerPositivo: Int = 0,
    var cornerNegativo: Int = 0,
    var tiroLibreFavor: Int = 0,
    var tiroLibreContra: Int = 0,
    var penalFavor: Int = 0,
    var penalContra: Int = 0,

    // Se mantienen para después
    var amarilla: Int = 0,
    var roja: Int = 0
)