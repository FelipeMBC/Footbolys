package com.fmarquez.footboly.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.modelos.MatchEvent
import com.fmarquez.footboly.modelos.MatchRecord
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.util.hexToColor
import com.fmarquez.footboly.util.teamColorLight
import com.fmarquez.footboly.vm.FutbolViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val BgColor = Color(0xFFF7F7F5)
private val SurfaceColor = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF111111)
private val TextSecondary = Color(0xFF888888)
private val BorderColor = Color(0xFFE0E0DC)
private val ErrorRed = Color(0xFFD32F2F)
private val ErrorRedLight = Color(0xFFFFF1F1)
private val InjuryAmber = Color(0xFFE65100)
private val InjuryLight = Color(0xFFFFF3E0)
private val BlockedGray = Color(0xFFEAEAEA)
private val BlockedText = Color(0xFF8C8C8C)
private val YellowCardColor = Color(0xFFF2C94C)
private val RedCardColor = Color(0xFFE53935)

private data class EventSummaryItem(
    val originalType: String,
    val displayType: String,
    val total: Int,
    val players: List<Pair<String, Int>>,
    val detailLines: List<String>
)

private fun parseEventCount(detail: String): Int {
    return detail.substringAfter(": ", "").toIntOrNull() ?: 1
}

private fun currentTeamLabel(match: MatchRecord?): String {
    return match?.teamName?.ifBlank { "Mi Equipo" } ?: "Mi Equipo"
}

private fun currentRivalLabel(match: MatchRecord?): String {
    return match?.rivalName?.ifBlank { "Visita" } ?: "Visita"
}

private fun completedAtMillis(match: MatchRecord): Long {
    return match.finishedAtMillis ?: match.createdAtMillis
}

private fun completedDateText(match: MatchRecord): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(completedAtMillis(match)))
}

private fun completedTimeText(match: MatchRecord): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(completedAtMillis(match)))
}

private fun actualMatchDurationSeconds(match: MatchRecord): Int {
    return (match.totalSeconds - match.remainingSeconds).coerceAtLeast(0)
}

private fun actualMatchDurationText(match: MatchRecord): String {
    val total = actualMatchDurationSeconds(match)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60

    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private fun isTeamGoalType(type: String, match: MatchRecord?): Boolean {
    val teamName = currentTeamLabel(match)
    return type == "Gol a Favor" || type == "Gol $teamName" || type == "Gol de $teamName"
}


private fun isOpponentGoalType(type: String, match: MatchRecord?): Boolean {
    val rivalName = currentRivalLabel(match)
    return type == "Gol en Contra" || type == "Gol Rival" || type == "Gol $rivalName" || type == "Gol de $rivalName"
}

private fun buildNarrativeLine(
    type: String,
    playerName: String,
    match: MatchRecord?
): String {
    val teamName = currentTeamLabel(match)
    val rivalName = currentRivalLabel(match)
    val safePlayerName = playerName.ifBlank { "Jugador" }

    return when {
        type == "Amarilla" ->
            "“$safePlayerName” es amonestado con tarjeta amarilla."

        type == "Doble Amarilla" ->
            "“$safePlayerName” es expulsado por doble amarilla."

        type == "Roja" ->
            "“$safePlayerName” es expulsado por roja directa."

        isTeamGoalType(type, match) ->
            "Gooooool de $teamName, “$safePlayerName” anota un golazo."

        isOpponentGoalType(type, match) ->
            "Gooooool de $rivalName, el rival convierte para su equipo."

        type == "Asistencia a favor" || type == "Participación Gol de $teamName" ->
            "Asistencia de gol para “$safePlayerName”."

        type == "Asistencia en contra" || type == "Participación Gol de $rivalName" ->
            "Falla de “$safePlayerName” en la defensa de $teamName."

        type == "Tiro al Arco +" ->
            "$teamName se aproxima al arco de $rivalName, posibilidad de gol de “$safePlayerName”."

        type == "Tiro al Arco -" ->
            "“$safePlayerName” intenta al arco, pero la jugada no termina bien."

        type == "Remate 1/2 +" ->
            "“$safePlayerName” intenta de media distancia al arco rival."

        type == "Remate 1/2 -" ->
            "“$safePlayerName” prueba de media distancia, pero el remate no es efectivo."

        type == "Balón Recogido a Favor" || type == "Balón Recuperado" ->
            "“$safePlayerName” recupera el balón."

        type == "Balón Recogido en Contra" || type == "Balón Perdido" ->
            "Balón perdido por “$safePlayerName”."

        type == "Pases Malos" ->
            "Falla en el pase “$safePlayerName”."

        type == "Pases Buenos" ->
            "“$safePlayerName” da un gran pase para $teamName."

        type == "Centros +" ->
            "$teamName se aproxima con “$safePlayerName”, que busca el área a través de un gran centro."

        type == "Centros -" ->
            "$teamName busca el área, pero el centro de “$safePlayerName” no es bueno."

        type == "Rechazos +" ->
            "“$safePlayerName” realiza un gran rechazo para salvar el peligro."

        type == "Rechazos -" ->
            "Ataque del $rivalName y “$safePlayerName” falla en el rechazo."

        type == "Penal a Favor" || type == "Penal para $teamName" ->
            "Falta del jugador del $rivalName a “$safePlayerName” y el árbitro cobra penal para $teamName."

        type == "Penal en Contra" || type == "Penal para $rivalName" ->
            "“$safePlayerName” comete falta en el área y el árbitro cobra penal para $rivalName."

        type == "Falta a Favor" || type == "Falta para $teamName" ->
            "“$safePlayerName” cuida el balón y recibe falta."

        type == "Falta en Contra" || type == "Falta para $rivalName" ->
            "Foul de “$safePlayerName”, el árbitro cobra la falta para $rivalName."

        type == "Corner +" ->
            "“$safePlayerName” busca el área desde el corner."

        type == "Corner -" ->
            "“$safePlayerName” envía el balón afuera, corner para $rivalName."

        type == "Tiro Libre a Favor" || type == "Off Side a Favor" || type == "Off Side para $teamName" ->
            "Ataque de “$rivalName” pero no vale, fuera de Juego para $teamName"

        type == "Tiro Libre en Contra" || type == "Off Side en Contra" || type == "Off Side para $rivalName" ->
            "Cuando “$safePlayerName” definía para $teamName, el árbitro cobra fuera de juego en favor de “$rivalName”"

        type == "Oportunidad de Gol Rival" || type == "Oportunidad de Gol de $rivalName" ->
            "$rivalName se aproxima al arco de $teamName con una clara oportunidad de gol."

        type == "Cambio" ->
            "Cambio registrado en $teamName."

        else ->
            "Evento registrado: ${normalizeEventType(type, match)}."
    }
}

private fun buildDetailLines(
    events: List<MatchEvent>,
    match: MatchRecord?
): List<String> {
    return events.flatMap { event ->
        val count = parseEventCount(event.detail).coerceAtLeast(1)
        List(count) {
            buildNarrativeLine(
                type = event.type,
                playerName = event.playerName,
                match = match
            )
        }
    }
}

private fun normalizeEventType(type: String, match: MatchRecord? = null): String {
    val teamName = currentTeamLabel(match)
    val rivalName = currentRivalLabel(match)

    return when (type) {
        "Gol a Favor", "Gol $teamName" -> "Gol de $teamName"
        "Gol en Contra", "Gol Rival", "Gol $rivalName" -> "Gol de $rivalName"
        "Asistencia a favor" -> "Participación Gol de $teamName"
        "Asistencia en contra" -> "Participación Gol de $rivalName"
        "Balón Recogido a Favor" -> "Balón Recuperado"
        "Balón Recogido en Contra" -> "Balón Perdido"
        "Falta a Favor" -> "Falta para $teamName"
        "Falta en Contra" -> "Falta para $rivalName"
        "Tiro Libre a Favor", "Off Side a Favor" -> "Off Side para $teamName"
        "Tiro Libre en Contra", "Off Side en Contra" -> "Off Side para $rivalName"
        "Penal a Favor" -> "Penal para $teamName"
        "Penal en Contra" -> "Penal para $rivalName"
        "Oportunidad de Gol Rival", "Oportunidad de Gol $rivalName" -> "Oportunidad de Gol de $rivalName"
        else -> type
    }
}

private fun expandEventsForTimeline(events: List<MatchEvent>): List<MatchEvent> {
    return events.flatMap { event ->
        val count = parseEventCount(event.detail).coerceAtLeast(1)

        List(count) {
            event.copy(
                detail = ""
            )
        }
    }
}

private fun playerCardCounts(
    playerId: Int,
    events: List<MatchEvent>
): Pair<Int, Int> {
    val yellow = events
        .filter { it.playerId == playerId && it.type == "Amarilla" }
        .sumOf { parseEventCount(it.detail) }
        .coerceAtMost(2)

    val red = events
        .filter { it.playerId == playerId && it.type == "Roja" }
        .sumOf { parseEventCount(it.detail) }
        .coerceAtMost(1)

    return yellow to red
}

private fun buildEventSummary(
    events: List<MatchEvent>,
    match: MatchRecord? = null
): List<EventSummaryItem> {
    val grouped = events.groupBy { normalizeEventType(it.type, match) }

    return grouped.map { (displayType, typeEvents) ->
        val total = typeEvents.sumOf { parseEventCount(it.detail) }

        val players = typeEvents
            .groupBy { it.playerName.ifBlank { "Sin jugador" } }
            .map { (playerName, playerEvents) ->
                val playerTotal = playerEvents.sumOf { parseEventCount(it.detail) }
                playerName to playerTotal
            }
            .sortedBy { it.first }

        EventSummaryItem(
            originalType = typeEvents.firstOrNull()?.type.orEmpty(),
            displayType = displayType,
            total = total,
            players = players,
            detailLines = buildDetailLines(typeEvents, match)
        )
    }.sortedBy { it.displayType }
}

private fun teamGoals(match: MatchRecord): Int {
    return match.events
        .filter { isTeamGoalType(it.type, match) }
        .sumOf { parseEventCount(it.detail) }
}

private fun totalExpandedEvents(match: MatchRecord): Int {
    return match.events.sumOf { parseEventCount(it.detail) }
}

private fun matchHistorySubtitle(match: MatchRecord): String {
    val rival = match.rivalName.ifBlank { "Sin rival" }
    val goals = teamGoals(match)
    return "${match.teamName} $goals - ${match.opponentGoals} $rival"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchTimelineScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val selectedMatch = vm.selectedFinishedMatch
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showAllDetailsDialog by remember { mutableStateOf(false) }
    var matchToDelete by remember { mutableStateOf<MatchRecord?>(null) }
    var selectedEventSummary by remember { mutableStateOf<EventSummaryItem?>(null) }
    var matchToExport by remember { mutableStateOf<MatchRecord?>(null) }
    var exportedMatchJson by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportIncludeFullTeam by rememberSaveable { mutableStateOf(false) }
    var importIncludeFullTeam by rememberSaveable { mutableStateOf(false) }
    var importJsonText by rememberSaveable { mutableStateOf("") }

    val summarizedEvents = selectedMatch?.let { buildEventSummary(it.events, it) } ?: emptyList()

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedMatch == null) "Partidos" else "Detalle del partido",
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedMatch != null) vm.clearSelectedFinishedMatch()
                            else navHostController.popBackStack()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                },
                actions = {
                    if (selectedMatch == null) {
                        TextButton(onClick = { showImportDialog = true }) {
                            Text(
                                text = "Importar",
                                color = TextPrimary,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
            )
        }
    ) { padding ->
        if (selectedMatch == null) {
            MatchHistoryList(
                matches = vm.finishedMatches.sortedByDescending { it.finishedAtMillis ?: it.createdAtMillis },
                onSelectMatch = { vm.selectFinishedMatch(it) },
                onEditMatch = { vm.selectFinishedMatch(it) },
                onExportMatch = {
                    matchToExport = it
                    exportIncludeFullTeam = false
                    exportedMatchJson = vm.exportMatchToJson(it, includeFullTeam = false)
                    showExportDialog = true
                },
                onDeleteMatch = { matchToDelete = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            )
        } else {
            MatchDetailContent(
                match = selectedMatch,
                vm = vm,
                onShowAllDetails = { showAllDetailsDialog = true },
                onEditPlayerStats = { player ->
                    vm.startEditingPlayerFromFinishedMatch(selectedMatch, player.id)
                    navHostController.navigate(Screen.PLAYER_STATS.route)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
    }

    if (showAllDetailsDialog && selectedMatch != null) {
        val matchColor = hexToColor(selectedMatch.shirtColorHex)
        val matchColorLight = teamColorLight(matchColor)

        AlertDialog(
            onDismissRequest = { showAllDetailsDialog = false },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Totales del partido",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (summarizedEvents.isEmpty()) {
                        Text("No hay eventos registrados", color = TextSecondary)
                    } else {
                        summarizedEvents.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedEventSummary = item }
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(matchColorLight, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = eventIcon(item.originalType, match = selectedMatch),
                                            contentDescription = item.displayType,
                                            tint = matchColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.displayType,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Toca para ver detalle",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(BgColor, RoundedCornerShape(8.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = item.total.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllDetailsDialog = false }) {
                    Text("Cerrar", color = matchColor)
                }
            }
        )
    }

    if (selectedEventSummary != null) {
        val selectedMatchColor = selectedMatch?.let { hexToColor(it.shirtColorHex) } ?: Color(0xFF1E6B45)
        val selectedMatchColorLight = teamColorLight(selectedMatchColor)
        val eventItem = selectedEventSummary!!

        AlertDialog(
            onDismissRequest = { selectedEventSummary = null },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = eventItem.displayType,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Total: ${eventItem.total}",
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    if (eventItem.detailLines.isEmpty()) {
                        Text("No hay detalle disponible para este evento", color = TextSecondary)
                    } else {
                        eventItem.detailLines.forEachIndexed { index, line ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(selectedMatchColorLight, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = selectedMatchColor,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = eventIcon(eventItem.originalType, match = selectedMatch),
                                                contentDescription = eventItem.displayType,
                                                tint = selectedMatchColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Detalle",
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = line,
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEventSummary = null }) {
                    Text("Cerrar", color = selectedMatchColor)
                }
            }
        )
    }


    if (showExportDialog && matchToExport != null) {
        AlertDialog(
            onDismissRequest = {
                showExportDialog = false
                matchToExport = null
            },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Exportar JSON",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Elige qué deseas incluir en el JSON.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    JsonTransferModeOption(
                        selected = !exportIncludeFullTeam,
                        title = "Solo historial del partido",
                        description = "Exporta el partido, sus eventos, minutos y alineación de ese encuentro.",
                        onClick = {
                            exportIncludeFullTeam = false
                            exportedMatchJson = vm.exportMatchToJson(matchToExport!!, includeFullTeam = false)
                        }
                    )

                    JsonTransferModeOption(
                        selected = exportIncludeFullTeam,
                        title = "Equipo completo + historial del partido",
                        description = "Agrega además la ficha del equipo y su plantilla completa al JSON.",
                        onClick = {
                            exportIncludeFullTeam = true
                            exportedMatchJson = vm.exportMatchToJson(matchToExport!!, includeFullTeam = true)
                        }
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BgColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = exportedMatchJson,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExportDialog = false
                        matchToExport = null
                    }
                ) {
                    Text("Cerrar", color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportedMatchJson))
                        Toast.makeText(
                            context,
                            if (exportIncludeFullTeam) "JSON del equipo y partido copiado" else "JSON del partido copiado",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text("Copiar", color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Importar JSON",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pega aquí el JSON exportado desde otro celular y elige cómo deseas importarlo.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 320.dp),
                        minLines = 8,
                        maxLines = 14,
                        shape = RoundedCornerShape(12.dp)
                    )

                    JsonTransferModeOption(
                        selected = !importIncludeFullTeam,
                        title = "Importar solo historial del partido",
                        description = "Guarda únicamente el partido en el equipo actualmente seleccionado.",
                        onClick = { importIncludeFullTeam = false }
                    )

                    JsonTransferModeOption(
                        selected = importIncludeFullTeam,
                        title = "Importar equipo completo + historial",
                        description = "Crea o actualiza el equipo del JSON y luego importa el partido.",
                        onClick = { importIncludeFullTeam = true }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.importMatchFromJson(
                            jsonText = importJsonText,
                            includeFullTeam = importIncludeFullTeam,
                            onSuccess = {
                                showImportDialog = false
                                importJsonText = ""
                                Toast.makeText(
                                    context,
                                    if (importIncludeFullTeam) {
                                        "Equipo y partido importados correctamente"
                                    } else {
                                        "Historial del partido importado correctamente"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onError = { errorMessage ->
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Text("Importar", color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
            }
        )
    }

    if (matchToDelete != null) {
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Eliminar partido", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = TextPrimary) },
            text = { Text("¿Eliminar este partido guardado?", color = TextSecondary) },
            dismissButton = {
                TextButton(onClick = { matchToDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteFinishedMatch(matchToDelete!!.id)
                        matchToDelete = null
                    }
                ) {
                    Text("Eliminar", color = ErrorRed, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                }
            }
        )
    }

    if (vm.shouldShowEditResultDialog) {
        val matchColor = selectedMatch?.let { hexToColor(it.shirtColorHex) } ?: Color(0xFF1E6B45)
        AlertDialog(
            onDismissRequest = { vm.dismissEditResultDialog() },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Cambios realizados", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    if (vm.lastEditChanges.isEmpty()) {
                        Text("No hubo cambios", color = TextSecondary)
                    } else {
                        vm.lastEditChanges.forEach { line ->
                            Text(line, fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.dismissEditResultDialog() }) {
                    Text("Aceptar", color = matchColor)
                }
            }
        )
    }
}

@Composable
private fun JsonTransferModeOption(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (selected) TextPrimary else BorderColor,
                RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) BgColor else SurfaceColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.RadioButtonChecked else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (selected) TextPrimary else TextSecondary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun MatchHistoryList(
    matches: List<MatchRecord>,
    onSelectMatch: (MatchRecord) -> Unit,
    onEditMatch: (MatchRecord) -> Unit,
    onExportMatch: (MatchRecord) -> Unit,
    onDeleteMatch: (MatchRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    if (matches.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏟️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Sin partidos guardados", color = TextSecondary, fontSize = 15.sp)
            }
        }
    } else {
        LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(matches, key = { it.id }) { match ->
                val matchColor = hexToColor(match.shirtColorHex)
                val matchColorLight = teamColorLight(matchColor)

                var expanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMatch(match) }
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(matchColorLight, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚽", fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = matchHistorySubtitle(match),
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = completedDateText(match),
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Duración: ${actualMatchDurationText(match)}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.TrackChanges,
                                    contentDescription = null,
                                    tint = matchColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Eventos: ${totalExpandedEvents(match)}",
                                    fontSize = 12.sp,
                                    color = matchColor,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(matchColor, CircleShape)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = TextSecondary)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(SurfaceColor)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Editar", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary)
                                    },
                                    onClick = {
                                        expanded = false
                                        onEditMatch(match)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Exportar JSON", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Send, contentDescription = null, tint = TextSecondary)
                                    },
                                    onClick = {
                                        expanded = false
                                        onExportMatch(match)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar", color = ErrorRed) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed)
                                    },
                                    onClick = {
                                        expanded = false
                                        onDeleteMatch(match)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchDetailContent(
    match: MatchRecord,
    vm: FutbolViewModel,
    onShowAllDetails: () -> Unit,
    onEditPlayerStats: (Player) -> Unit,
    modifier: Modifier = Modifier
) {
    val starters = match.starters.sortedBy { it.number }
    val substitutes = match.substitutes.sortedBy { it.number }
    val expelledPlayers = match.expelledPlayers.sortedBy { it.number }
    val injuredPlayers = match.injuredPlayers.sortedBy { it.number }

    val teamColor = hexToColor(match.shirtColorHex)
    val teamColorLight = teamColorLight(teamColor)
    val expandedTimelineEvents = remember(match.events) { expandEventsForTimeline(match.events) }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            val teamGoalsValue = teamGoals(match)
            val rivalNameLabel = match.rivalName.ifBlank { "Visita" }
            val completedDate = completedDateText(match)
            val completedTime = completedTimeText(match)
            val completedDuration = actualMatchDurationText(match)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, teamColor.copy(alpha = 0.12f), RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(teamColorLight.copy(alpha = 0.55f))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Partido terminado",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 16.sp,
                            color = teamColor
                        )
                        Text(
                            text = "Resumen general",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 18.dp)
                        ) {
                            val compact = maxWidth < 330.dp
                            val teamNameFont = if (compact) 11.sp else 15.sp
                            val roleFont = if (compact) 11.sp else 12.sp
                            val scoreFont = if (compact) 24.sp else 28.sp
                            val dashFont = if (compact) 18.sp else 20.sp
                            val scoreSpacing = if (compact) 6.dp else 10.dp
                            val sidePadding = if (compact) 4.dp else 0.dp

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = sidePadding),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = match.teamName,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = teamNameFont,
                                        color = TextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Local",
                                        fontSize = roleFont,
                                        color = TextSecondary
                                    )
                                }

                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(scoreSpacing)
                                ) {
                                    Text(
                                        text = teamGoalsValue.toString(),
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = scoreFont,
                                        color = teamColor
                                    )

                                    Text(
                                        text = "-",
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                        fontSize = dashFont,
                                        color = TextSecondary
                                    )

                                    Text(
                                        text = match.opponentGoals.toString(),
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = scoreFont,
                                        color = TextPrimary
                                    )
                                }

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = sidePadding),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = rivalNameLabel,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = teamNameFont,
                                        color = TextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Visita",
                                        fontSize = roleFont,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryRow("Cantidad Eventos", "${expandedTimelineEvents.size}", teamColor)
                        SummaryRow("Fecha", completedDate, teamColor)
                        SummaryRow("Hora", completedTime, teamColor)
                        SummaryRow("Duración", completedDuration, teamColor)
                    }
                }
            }
        }

        item {
            Button(
                onClick = onShowAllDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver todos los eventos", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            }
        }

        item {
            Text("Jugadores del partido", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Titulares",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(teamColorLight, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text("${starters.size}", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = teamColor)
                }
            }
        }

        if (starters.isEmpty()) {
            item { Text("No hubo titulares registrados", color = TextSecondary) }
        } else {
            items(starters, key = { it.id }) { player ->
                val (yellowCards, redCards) = playerCardCounts(player.id, match.events)

                SavedMatchPlayerRow(
                    player = player,
                    role = "Titular",
                    playedTime = vm.getFormattedPlayerTime(player.id, match),
                    teamColor = teamColor,
                    teamColorLight = teamColorLight,
                    yellowCards = yellowCards,
                    redCards = redCards,
                    onEditPlayerStats = { onEditPlayerStats(player) }
                )
            }
        }

        if (expelledPlayers.isNotEmpty()) {
            item {
                BlockedSectionHeader(
                    title = "Expulsados",
                    count = expelledPlayers.size,
                    accentColor = ErrorRed,
                    accentLight = ErrorRedLight
                )
            }

            items(expelledPlayers, key = { it.id }) { player ->
                val (yellowCards, redCards) = playerCardCounts(player.id, match.events)

                SavedBlockedMatchPlayerRow(
                    player = player,
                    reason = "Expulsado",
                    accentColor = ErrorRed,
                    yellowCards = yellowCards,
                    redCards = redCards
                )
            }
        }

        if (injuredPlayers.isNotEmpty()) {
            item {
                BlockedSectionHeader(
                    title = "Lesionados",
                    count = injuredPlayers.size,
                    accentColor = InjuryAmber,
                    accentLight = InjuryLight
                )
            }

            items(injuredPlayers, key = { it.id }) { player ->
                val (yellowCards, redCards) = playerCardCounts(player.id, match.events)

                SavedBlockedMatchPlayerRow(
                    player = player,
                    reason = "Lesionado",
                    accentColor = InjuryAmber,
                    yellowCards = yellowCards,
                    redCards = redCards
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Reservas",
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text("${substitutes.size}", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = TextSecondary)
                }
            }
        }

        if (substitutes.isEmpty()) {
            item { Text("No hubo reservas registradas", color = TextSecondary) }
        } else {
            items(substitutes, key = { it.id }) { player ->
                val (yellowCards, redCards) = playerCardCounts(player.id, match.events)

                SavedMatchPlayerRow(
                    player = player,
                    role = "Reserva",
                    playedTime = vm.getFormattedPlayerTime(player.id, match),
                    teamColor = teamColor,
                    teamColorLight = teamColorLight,
                    yellowCards = yellowCards,
                    redCards = redCards,
                    onEditPlayerStats = { onEditPlayerStats(player) }
                )
            }
        }


        item {
            MatchInteractiveTimelineSection(
                match = match,
                events = expandedTimelineEvents,
                teamColor = teamColor,
                teamColorLight = teamColorLight
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

private enum class TimelinePeriod(
    val label: String,
    val startMinute: Int,
    val endMinute: Int
) {
    FIRST_HALF("Primer tiempo", 0, 45),
    SECOND_HALF("Segundo tiempo", 45, 90),
    FULL_MATCH("Partido completo", 0, 90);

    val totalMinutes: Int
        get() = (endMinute - startMinute).coerceAtLeast(1)
}

private data class TimelineCategoryOption(
    val id: String,
    val label: String
)

private data class TimelineEventPlacement(
    val event: MatchEvent,
    val minute: Int,
    val lane: Int,
    val categoryId: String
)

private fun timelineCategoryOptions(): List<TimelineCategoryOption> {
    return listOf(
        TimelineCategoryOption("all", "Todas"),
        TimelineCategoryOption("goals", "Goles"),
        TimelineCategoryOption("assists", "Asistencias"),
        TimelineCategoryOption("shots", "Remates"),
        TimelineCategoryOption("fouls", "Faltas"),
        TimelineCategoryOption("cards", "Tarjetas"),
        TimelineCategoryOption("changes", "Cambios"),
        TimelineCategoryOption("others", "Otros")
    )
}

private fun timelineCategoryIdForEvent(event: MatchEvent, match: MatchRecord): String {
    val normalizedType = normalizeEventType(event.type, match)

    return when {
        normalizedType.startsWith("Gol de") -> "goals"
        normalizedType.startsWith("Particip") -> "assists"
        normalizedType.startsWith("Tiro al Arco") ||
                normalizedType.startsWith("Remate 1/2") ||
                normalizedType.startsWith("Oportunidad de Gol") -> "shots"

        normalizedType == "Amarilla" ||
                normalizedType == "Doble Amarilla" ||
                normalizedType == "Roja" -> "cards"

        normalizedType == "Cambio" -> "changes"
        normalizedType.startsWith("Falta para") ||
                normalizedType.startsWith("Off Side para") ||
                normalizedType.startsWith("Penal para") ||
                normalizedType.startsWith("Corner") -> "fouls"

        else -> "others"
    }
}

private fun timelineCategoryColor(categoryId: String, teamColor: Color): Color {
    return when (categoryId) {
        "goals" -> teamColor
        "assists" -> Color(0xFF1E88E5)
        "shots" -> Color(0xFFF57C00)
        "fouls" -> Color(0xFF6D4C41)
        "cards" -> Color(0xFFE53935)
        "changes" -> Color(0xFF546E7A)
        else -> TextSecondary
    }
}

private fun eventBelongsToPeriod(minute: Int, period: TimelinePeriod): Boolean {
    return when (period) {
        TimelinePeriod.FULL_MATCH -> true
        TimelinePeriod.FIRST_HALF -> minute <= 45
        TimelinePeriod.SECOND_HALF -> minute >= 45
    }
}

private fun minuteInsidePeriod(minute: Int, period: TimelinePeriod): Int {
    val safeMinute = minute.coerceAtLeast(0)
    return safeMinute.coerceIn(period.startMinute, period.endMinute)
}

private fun timelineTickMinutes(period: TimelinePeriod): List<Int> {
    return when (period) {
        TimelinePeriod.FULL_MATCH -> listOf(0, 15, 30, 45, 60, 75, 90)
        TimelinePeriod.FIRST_HALF -> listOf(0, 15, 30, 45)
        TimelinePeriod.SECOND_HALF -> listOf(45, 60, 75, 90)
    }
}

private fun buildTimelinePlacements(
    events: List<MatchEvent>,
    match: MatchRecord,
    categoryId: String,
    period: TimelinePeriod,
    zoomLevel: Float
): List<TimelineEventPlacement> {
    val laneLastMinute = mutableListOf<Float>()
    val minMinuteGap = (2.4f / zoomLevel).coerceAtLeast(0.5f)

    return events.asSequence()
        .filter { eventBelongsToPeriod(it.minute, period) }
        .map { event ->
            val resolvedCategory = timelineCategoryIdForEvent(event, match)
            val resolvedMinute = minuteInsidePeriod(event.minute, period)
            Triple(event, resolvedCategory, resolvedMinute)
        }
        .filter { (_, resolvedCategory, _) ->
            categoryId == "all" || resolvedCategory == categoryId
        }
        .sortedWith(
            compareBy<Triple<MatchEvent, String, Int>> { it.third }
                .thenBy { it.first.playerName.lowercase() }
                .thenBy { it.first.type.lowercase() }
        )
        .map { (event, resolvedCategory, resolvedMinute) ->
            var laneIndex = -1
            for (index in laneLastMinute.indices) {
                if (resolvedMinute - laneLastMinute[index] >= minMinuteGap) {
                    laneIndex = index
                    break
                }
            }

            if (laneIndex == -1) {
                laneLastMinute.add(resolvedMinute.toFloat())
                laneIndex = laneLastMinute.lastIndex
            } else {
                laneLastMinute[laneIndex] = resolvedMinute.toFloat()
            }

            TimelineEventPlacement(
                event = event,
                minute = resolvedMinute,
                lane = laneIndex,
                categoryId = resolvedCategory
            )
        }
        .toList()
}

private fun timelineXOffset(
    minute: Int,
    period: TimelinePeriod,
    axisStart: androidx.compose.ui.unit.Dp,
    axisWidth: androidx.compose.ui.unit.Dp
): androidx.compose.ui.unit.Dp {
    val fraction = ((minute - period.startMinute).toFloat() / period.totalMinutes.toFloat()).coerceIn(0f, 1f)
    return axisStart + axisWidth * fraction
}

@Composable
private fun MatchInteractiveTimelineSection(
    match: MatchRecord,
    events: List<MatchEvent>,
    teamColor: Color,
    teamColorLight: Color
) {
    val categoryOptions = remember { timelineCategoryOptions() }
    val periodOptions = remember { TimelinePeriod.values().toList() }

    var selectedCategoryId by rememberSaveable(match.id) { mutableStateOf("all") }
    var selectedPeriodKey by rememberSaveable(match.id) { mutableStateOf(TimelinePeriod.FULL_MATCH.name) }
    var zoomLevel by rememberSaveable(match.id) { mutableStateOf(1f) }
    var selectedMarker by remember { mutableStateOf<TimelineEventPlacement?>(null) }

    val selectedPeriod = TimelinePeriod.valueOf(selectedPeriodKey)

    val categoryCountById = remember(events, match) {
        events.groupingBy { timelineCategoryIdForEvent(it, match) }.eachCount()
    }

    val placements = remember(events, match, selectedCategoryId, selectedPeriod, zoomLevel) {
        buildTimelinePlacements(
            events = events,
            match = match,
            categoryId = selectedCategoryId,
            period = selectedPeriod,
            zoomLevel = zoomLevel
        )
    }

    val selectedCategoryLabel = categoryOptions.firstOrNull { it.id == selectedCategoryId }?.label ?: "Todas"

    val zoomScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Linea de tiempo",
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimelineFilterDropdown(
                    title = "Categoria",
                    selectedText = selectedCategoryLabel,
                    options = categoryOptions.map { option ->
                        val count = if (option.id == "all") {
                            events.size
                        } else {
                            categoryCountById[option.id] ?: 0
                        }
                        option.id to "${option.label} ($count)"
                    },
                    onSelect = { selectedCategoryId = it },
                    modifier = Modifier.weight(1f)
                )

                TimelineFilterDropdown(
                    title = "Partido",
                    selectedText = selectedPeriod.label,
                    options = periodOptions.map { option -> option.name to option.label },
                    onSelect = {
                        selectedPeriodKey = it
                        scope.launch { zoomScrollState.scrollTo(0) }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${placements.size} eventos",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { zoomLevel = (zoomLevel - 0.5f).coerceAtLeast(1f) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text("-", fontSize = 14.sp)
                    }

                    Text(
                        text = "${(zoomLevel * 100f).roundToInt()}%",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    OutlinedButton(
                        onClick = { zoomLevel = (zoomLevel + 0.5f).coerceAtMost(4f) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Text("+", fontSize = 14.sp)
                    }

                    TextButton(
                        onClick = {
                            zoomLevel = 1f
                            scope.launch { zoomScrollState.scrollTo(0) }
                        }
                    ) {
                        Text("Ajustar", fontSize = 12.sp, color = teamColor)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, teamColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = teamColorLight.copy(alpha = 0.32f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (placements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay eventos para los filtros seleccionados.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        val viewportWidth = maxWidth
                        val contentWidth = (viewportWidth * zoomLevel).coerceAtLeast(viewportWidth)
                        val axisStart = 24.dp
                        val axisEnd = 24.dp
                        val axisWidth = (contentWidth - axisStart - axisEnd).coerceAtLeast(80.dp)
                        val laneSpacing = 30.dp
                        val laneCount = (placements.maxOfOrNull { it.lane } ?: -1) + 1
                        val safeLaneCount = laneCount.coerceAtLeast(1)
                        val timelineHeight = 120.dp + laneSpacing * safeLaneCount.toFloat()
                        val axisY = timelineHeight - 40.dp
                        val tickMinutes = timelineTickMinutes(selectedPeriod)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(zoomScrollState)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(contentWidth)
                                    .height(timelineHeight)
                            ) {
                                if (selectedPeriod == TimelinePeriod.FULL_MATCH) {
                                    val halfX = timelineXOffset(45, selectedPeriod, axisStart, axisWidth)
                                    Text(
                                        text = "1T",
                                        fontSize = 11.sp,
                                        color = teamColor,
                                        modifier = Modifier.offset(x = axisStart + 4.dp, y = axisY - 34.dp)
                                    )
                                    Text(
                                        text = "2T",
                                        fontSize = 11.sp,
                                        color = teamColor,
                                        modifier = Modifier.offset(x = halfX + 6.dp, y = axisY - 34.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .offset(x = halfX, y = axisY - 16.dp)
                                            .width(2.dp)
                                            .height(30.dp)
                                            .background(teamColor.copy(alpha = 0.45f))
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .offset(x = axisStart, y = axisY)
                                        .width(axisWidth)
                                        .height(2.dp)
                                        .background(BorderColor, RoundedCornerShape(1.dp))
                                )

                                tickMinutes.forEach { minute ->
                                    val tickX = timelineXOffset(minute, selectedPeriod, axisStart, axisWidth)
                                    val isHalfSeparator = selectedPeriod == TimelinePeriod.FULL_MATCH && minute == 45

                                    Box(
                                        modifier = Modifier
                                            .offset(x = tickX, y = axisY - 8.dp)
                                            .width(if (isHalfSeparator) 2.dp else 1.dp)
                                            .height(if (isHalfSeparator) 18.dp else 14.dp)
                                            .background(TextSecondary.copy(alpha = if (isHalfSeparator) 0.75f else 0.5f))
                                    )

                                    Text(
                                        text = "${minute}'",
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.offset(x = tickX - 12.dp, y = axisY + 10.dp)
                                    )
                                }

                                placements.forEach { placement ->
                                    val markerX = timelineXOffset(
                                        minute = placement.minute,
                                        period = selectedPeriod,
                                        axisStart = axisStart,
                                        axisWidth = axisWidth
                                    )
                                    val markerCenterY = axisY - 22.dp - laneSpacing * placement.lane.toFloat()
                                    val markerColor = timelineCategoryColor(placement.categoryId, teamColor)
                                    val connectorTop = markerCenterY + 12.dp
                                    val connectorHeight = (axisY - connectorTop).coerceAtLeast(0.dp)

                                    Box(
                                        modifier = Modifier
                                            .offset(x = markerX, y = connectorTop)
                                            .width(1.dp)
                                            .height(connectorHeight)
                                            .background(markerColor.copy(alpha = 0.4f))
                                    )

                                    Box(
                                        modifier = Modifier
                                            .offset(x = markerX - 12.dp, y = markerCenterY - 12.dp)
                                            .size(24.dp)
                                            .background(markerColor, CircleShape)
                                            .border(1.dp, Color.White, CircleShape)
                                            .clickable { selectedMarker = placement },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = eventIcon(placement.event.type, placement.event.detail, match),
                                            contentDescription = placement.event.type,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "Tip: usa zoom y desplazamiento horizontal para separar eventos cercanos.",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }

    selectedMarker?.let { marker ->
        val markerTitle = formatEventTitle(marker.event.type, marker.event.detail, match)
        val markerDetail = marker.event.detail.ifBlank { "Sin detalle adicional." }

        AlertDialog(
            onDismissRequest = { selectedMarker = null },
            containerColor = SurfaceColor,
            title = {
                Text(
                    text = markerTitle,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Minuto ${marker.minute}'", color = TextPrimary, fontSize = 13.sp)
                    Text(
                        "Jugador: ${marker.event.playerName.ifBlank { "Sin jugador" }}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text("Detalle: $markerDetail", color = TextSecondary, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMarker = null }) {
                    Text("Cerrar", color = teamColor)
                }
            }
        )
    }
}

@Composable
private fun TimelineFilterDropdown(
    title: String,
    selectedText: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = selectedText,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SavedMatchPlayerRow(
    player: Player,
    role: String,
    playedTime: String,
    teamColor: Color,
    teamColorLight: Color,
    yellowCards: Int = 0,
    redCards: Int = 0,
    onEditPlayerStats: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val compact = maxWidth < 330.dp

            if (compact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (role == "Titular") teamColorLight else Color(0xFFF5F5F5),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.number.toString(),
                                fontSize = 13.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = if (role == "Titular") teamColor else TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.name,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$role · N° ${player.number} · $playedTime",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (yellowCards > 0 || redCards > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                PlayerCardBadges(
                                    yellowCards = yellowCards,
                                    redCards = redCards
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onEditPlayerStats,
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", fontSize = 13.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (role == "Titular") teamColorLight else Color(0xFFF5F5F5),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = player.number.toString(),
                            fontSize = 13.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = if (role == "Titular") teamColor else TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = player.name,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$role · N° ${player.number} · $playedTime",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onEditPlayerStats,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockedSectionHeader(
    title: String,
    count: Int,
    accentColor: Color,
    accentLight: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accentLight, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = 13.sp,
            color = accentColor,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun PlayerCardBadges(
    yellowCards: Int,
    redCards: Int
) {
    if (yellowCards <= 0 && redCards <= 0) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(yellowCards) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 16.dp)
                    .background(YellowCardColor, RoundedCornerShape(2.dp))
            )
        }

        repeat(redCards) {
            Box(
                modifier = Modifier
                    .size(width = 12.dp, height = 16.dp)
                    .background(RedCardColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun SavedBlockedMatchPlayerRow(
    player: Player,
    reason: String,
    accentColor: Color,
    yellowCards: Int = 0,
    redCards: Int = 0
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BlockedGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.number.toString(),
                    fontSize = 13.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = BlockedText
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = BlockedText
                )
                Text(
                    text = "Bloqueado · $reason · N° ${player.number}",
                    fontSize = 12.sp,
                    color = accentColor
                )

                if (yellowCards > 0 || redCards > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    PlayerCardBadges(
                        yellowCards = yellowCards,
                        redCards = redCards
                    )
                }
            }

            Icon(
                imageVector = if (reason == "Expulsado") Icons.Default.Warning else Icons.Default.Healing,
                contentDescription = reason,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, teamColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = teamColor.copy(alpha = 0.7f))
        Text(value, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = teamColor)
    }
}

private fun extractDatePart(dateTimeLabel: String): String {
    return dateTimeLabel.substringBefore("·").trim().ifBlank { "Sin fecha" }
}

private fun extractTimePart(dateTimeLabel: String): String {
    return dateTimeLabel.substringAfter("·", "").trim()
}

@Composable
fun MatchEventReportCard(
    title: String,
    timeLabel: String,
    playerName: String,
    detail: String = "",
    accentColor: Color = Color(0xFF1E6B45),
    accentColorLight: Color = Color(0xFFE8F2EC)
) {
    val isSwap = title == "Cambio" || detail.startsWith("Entra ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(accentColorLight, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = eventIcon(title, detail, match = null),
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSwap) "Cambio" else title,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                if (isSwap && detail.isNotBlank()) {
                    Text(detail, fontSize = 12.sp, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(playerName.ifBlank { "Sin jugador" }, fontSize = 12.sp, color = TextSecondary)
                }
            }

            Box(
                modifier = Modifier
                    .background(BgColor, RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        timeLabel.ifBlank { "00:00" },
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }
            }
        }
    }
}

fun formatEventTitle(type: String, detail: String, match: MatchRecord? = null): String {
    val normalizedType = normalizeEventType(type, match)
    if (normalizedType == "Cambio") return "Cambio"
    return normalizedType
}

fun eventIcon(type: String, detail: String = "", match: MatchRecord? = null): ImageVector {
    val normalizedType = normalizeEventType(type, match)

    return when {
        normalizedType == "Cambio" || detail.startsWith("Entra ") -> Icons.Default.TrackChanges

        normalizedType.startsWith("Gol") -> Icons.Default.SportsSoccer
        normalizedType.startsWith("Oportunidad de Gol") -> Icons.Default.GpsFixed
        normalizedType.startsWith("Tiro al Arco") -> Icons.Default.GpsFixed
        normalizedType.startsWith("Participación Gol") -> Icons.Default.Send
        normalizedType.startsWith("Remate 1/2") -> Icons.Default.TrackChanges

        normalizedType.startsWith("Balón Recuperado") -> Icons.Default.Security
        normalizedType.startsWith("Balón Perdido") -> Icons.Default.Security
        normalizedType.startsWith("Pases Buenos") -> Icons.Default.Send
        normalizedType.startsWith("Pases Malos") -> Icons.Default.Close
        normalizedType.startsWith("Centros +") -> Icons.Default.NorthEast
        normalizedType.startsWith("Centros -") -> Icons.Default.SouthWest
        normalizedType.startsWith("Rechazos +") -> Icons.Default.CheckCircle
        normalizedType.startsWith("Rechazos -") -> Icons.Default.Cancel

        normalizedType.startsWith("Falta para") -> Icons.Default.CheckCircle
        normalizedType.startsWith("Falta para") -> Icons.Default.Cancel
        normalizedType.startsWith("Corner +") -> Icons.Default.NorthEast
        normalizedType.startsWith("Corner -") -> Icons.Default.SouthWest
        normalizedType.startsWith("Off Side para") -> Icons.Default.Cancel
        normalizedType.startsWith("Penal para") -> Icons.Default.SportsSoccer
        normalizedType.startsWith("Penal para") -> Icons.Default.HighlightOff

        normalizedType.startsWith("Doble Amarilla") -> Icons.Default.Warning
        normalizedType.startsWith("Amarilla") -> Icons.Default.Warning
        normalizedType.startsWith("Roja") -> Icons.Default.HighlightOff

        else -> Icons.Default.SportsSoccer
    }
}

