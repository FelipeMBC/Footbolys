package com.fmarquez.footboly.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    val type: String,
    val total: Int,
    val players: List<Pair<String, Int>>
)

private fun parseEventCount(detail: String): Int {
    return detail.substringAfter(": ", "").toIntOrNull() ?: 1
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

private fun buildEventSummary(events: List<MatchEvent>): List<EventSummaryItem> {
    val grouped = events.groupBy { it.type }

    return grouped.map { (type, typeEvents) ->
        val total = typeEvents.sumOf { parseEventCount(it.detail) }

        val players = typeEvents
            .groupBy { it.playerName.ifBlank { "Sin jugador" } }
            .map { (playerName, playerEvents) ->
                val playerTotal = playerEvents.sumOf { parseEventCount(it.detail) }
                playerName to playerTotal
            }
            .sortedBy { it.first }

        EventSummaryItem(
            type = type,
            total = total,
            players = players
        )
    }.sortedBy { it.type }
}

private fun teamGoals(match: MatchRecord): Int {
    return match.events
        .filter { it.type == "Gol a Favor" }
        .sumOf { parseEventCount(it.detail) }
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
    var showAllDetailsDialog by remember { mutableStateOf(false) }
    var matchToDelete by remember { mutableStateOf<MatchRecord?>(null) }
    var selectedEventSummary by remember { mutableStateOf<EventSummaryItem?>(null) }
    val summarizedEvents = selectedMatch?.let { buildEventSummary(it.events) } ?: emptyList()

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
            )
        }
    ) { padding ->
        if (selectedMatch == null) {
            MatchHistoryList(
                matches = vm.finishedMatches.sortedByDescending { it.finishedAtMillis ?: it.createdAtMillis },
                onSelectMatch = { vm.selectFinishedMatch(it) },
                onEditMatch = { vm.selectFinishedMatch(it) },
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
                                            imageVector = eventIcon(item.type),
                                            contentDescription = item.type,
                                            tint = matchColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.type,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Toca para ver jugadores",
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
                    text = eventItem.type,
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Total: ${eventItem.total}",
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    if (eventItem.players.isEmpty()) {
                        Text("No hay jugadores asociados", color = TextSecondary)
                    } else {
                        eventItem.players.forEach { (playerName, count) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(selectedMatchColorLight, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = eventIcon(eventItem.type),
                                            contentDescription = eventItem.type,
                                            tint = selectedMatchColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playerName,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    Text(
                                        text = count.toString(),
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = selectedMatchColor
                                    )
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
fun MatchHistoryList(
    matches: List<MatchRecord>,
    onSelectMatch: (MatchRecord) -> Unit,
    onEditMatch: (MatchRecord) -> Unit,
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
                                    text = match.matchDateLabel.ifBlank { match.finishedAtLabel.ifBlank { "Sin fecha" } },
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
                                    text = "Duración: ${match.totalSeconds / 60} min",
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
                                    text = "Eventos: ${match.events.size}",
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

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = teamColorLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Partido terminado", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 15.sp, color = teamColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow("Equipo", match.teamName, teamColor)
                    if (match.rivalName.isNotBlank()) {
                        SummaryRow("Rival", match.rivalName, teamColor)
                    }
                    if (match.matchDateLabel.isNotBlank()) {
                        SummaryRowWithIcon(
                            label = "Fecha",
                            value = match.matchDateLabel,
                            teamColor = teamColor
                        )
                    }
                    SummaryRow("Duración", "${match.totalSeconds / 60} min", teamColor)
                    SummaryRow("Eventos", "${match.events.size}", teamColor)
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
                SavedMatchPlayerRow(
                    player = player,
                    role = "Titular",
                    playedTime = vm.getFormattedPlayerTime(player.id, match),
                    teamColor = teamColor,
                    teamColorLight = teamColorLight,
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
                SavedMatchPlayerRow(
                    player = player,
                    role = "Reserva",
                    playedTime = vm.getFormattedPlayerTime(player.id, match),
                    teamColor = teamColor,
                    teamColorLight = teamColorLight,
                    onEditPlayerStats = { onEditPlayerStats(player) }
                )
            }
        }

        item {
            Text("Línea de tiempo", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        }

        if (match.events.isEmpty()) {
            item { Text("No hay eventos registrados", color = TextSecondary) }
        } else {
            items(match.events) { event ->
                MatchEventReportCard(
                    title = formatEventTitle(event.type, event.detail),
                    timeLabel = event.timestampLabel,
                    playerName = event.playerName,
                    detail = event.detail,
                    accentColor = teamColor,
                    accentColorLight = teamColorLight
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun SavedMatchPlayerRow(
    player: Player,
    role: String,
    playedTime: String,
    teamColor: Color,
    teamColorLight: Color,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(if (role == "Titular") teamColorLight else Color(0xFFF5F5F5), CircleShape),
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
                Text(player.name, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                Text("$role · N° ${player.number} · $playedTime", fontSize = 12.sp, color = TextSecondary)
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

@Composable
private fun SummaryRowWithIcon(label: String, value: String, teamColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = teamColor.copy(alpha = 0.7f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = teamColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = teamColor)
        }
    }
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
                    imageVector = eventIcon(title, detail),
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

fun formatEventTitle(type: String, detail: String): String {
    if (type == "Cambio") return "Cambio"
    val count = detail.substringAfter(": ", "").toIntOrNull()
    return if (count != null) "$type - $count" else type
}

fun eventIcon(type: String, detail: String = ""): ImageVector {
    return when {
        type == "Cambio" || detail.startsWith("Entra ") -> Icons.Default.TrackChanges

        type.startsWith("Gol") -> Icons.Default.SportsSoccer
        type.startsWith("Tiro al Arco") -> Icons.Default.GpsFixed
        type.startsWith("Participación de Gol") -> Icons.Default.Send
        type.startsWith("Remate 1/2") -> Icons.Default.TrackChanges

        type.startsWith("Balón Recogido") -> Icons.Default.Security
        type.startsWith("Pases Buenos") -> Icons.Default.Send
        type.startsWith("Pases Malos") -> Icons.Default.Close
        type.startsWith("Centros +") -> Icons.Default.NorthEast
        type.startsWith("Centros -") -> Icons.Default.SouthWest
        type.startsWith("Rechazos +") -> Icons.Default.CheckCircle
        type.startsWith("Rechazos -") -> Icons.Default.Cancel

        type.startsWith("Falta a Favor") -> Icons.Default.CheckCircle
        type.startsWith("Falta en Contra") -> Icons.Default.Cancel
        type.startsWith("Corner +") -> Icons.Default.NorthEast
        type.startsWith("Corner -") -> Icons.Default.SouthWest
        type.startsWith("Tiro Libre a Favor") -> Icons.Default.RadioButtonChecked
        type.startsWith("Tiro Libre en Contra") -> Icons.Default.Cancel
        type.startsWith("Penal a Favor") -> Icons.Default.SportsSoccer
        type.startsWith("Penal en Contra") -> Icons.Default.HighlightOff

        type.startsWith("Doble Amarilla") -> Icons.Default.Warning
        type.startsWith("Amarilla") -> Icons.Default.Warning
        type.startsWith("Roja") -> Icons.Default.HighlightOff

        else -> Icons.Default.SportsSoccer
    }
}