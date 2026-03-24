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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.modelos.MatchRecord
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.vm.FutbolViewModel

private val BgColor          = Color(0xFFF7F7F5)
private val SurfaceColor     = Color(0xFFFFFFFF)
private val AccentGreen      = Color(0xFF1E6B45)
private val AccentGreenLight = Color(0xFFE8F2EC)
private val TextPrimary      = Color(0xFF111111)
private val TextSecondary    = Color(0xFF888888)
private val BorderColor      = Color(0xFFE0E0DC)
private val ErrorRed         = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchTimelineScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val selectedMatch = vm.selectedFinishedMatch
    var showAllDetailsDialog by remember { mutableStateOf(false) }
    var matchToDelete by remember { mutableStateOf<MatchRecord?>(null) }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedMatch == null) "Partidos" else "Detalle del partido",
                        fontWeight = FontWeight.SemiBold,
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
                matches = vm.finishedMatches,
                onSelectMatch = { vm.selectFinishedMatch(it) },
                onEditMatch = { vm.selectFinishedMatch(it) },
                onDeleteMatch = { matchToDelete = it },
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 16.dp)
            )
        } else {
            MatchDetailContent(
                match = selectedMatch,
                onShowAllDetails = { showAllDetailsDialog = true },
                onEditPlayerStats = { player ->
                    vm.startEditingPlayerFromFinishedMatch(selectedMatch, player.id)
                    navHostController.navigate(Screen.PLAYER_STATS.route)
                },
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
    }

    // ── Diálogos ──────────────────────────────────────────────────────────────
    if (showAllDetailsDialog && selectedMatch != null) {
        AlertDialog(
            onDismissRequest = { showAllDetailsDialog = false },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Todos los eventos", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    if (selectedMatch.events.isEmpty()) {
                        Text("No hay eventos registrados", color = TextSecondary)
                    } else {
                        selectedMatch.events.forEach { event ->
                            Text(
                                "${formatEventTitle(event.type, event.detail)} · ${event.playerName} · ${event.timestampLabel}",
                                fontSize = 13.sp, color = TextSecondary,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllDetailsDialog = false }) {
                    Text("Cerrar", color = AccentGreen)
                }
            }
        )
    }

    if (matchToDelete != null) {
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Eliminar partido", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("¿Eliminar este partido guardado?", color = TextSecondary) },
            dismissButton = { TextButton(onClick = { matchToDelete = null }) { Text("Cancelar", color = TextSecondary) } },
            confirmButton = {
                TextButton(
                    onClick = { vm.deleteFinishedMatch(matchToDelete!!.id); matchToDelete = null }
                ) { Text("Eliminar", color = ErrorRed, fontWeight = FontWeight.SemiBold) }
            }
        )
    }

    if (vm.shouldShowEditResultDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissEditResultDialog() },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Cambios realizados", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column {
                    if (vm.lastEditChanges.isEmpty()) Text("No hubo cambios", color = TextSecondary)
                    else vm.lastEditChanges.forEach { line -> Text(line, fontSize = 13.sp, color = TextSecondary) }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.dismissEditResultDialog() }) { Text("Aceptar", color = AccentGreen) }
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
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icono lateral
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentGreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚽", fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = match.teamName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(match.finishedAtLabel.ifBlank { "Sin fecha" }, fontSize = 12.sp, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${match.totalSeconds / 60} min · ${match.events.size} eventos", fontSize = 12.sp, color = TextSecondary)
                        }

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
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary) },
                                    onClick = { expanded = false; onEditMatch(match) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar", color = ErrorRed) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                                    onClick = { expanded = false; onDeleteMatch(match) }
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
    onShowAllDetails: () -> Unit,
    onEditPlayerStats: (Player) -> Unit,
    modifier: Modifier = Modifier
) {
    val starters = match.starters.sortedBy { it.number }
    val substitutes = match.substitutes.sortedBy { it.number }

    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ── Resumen del partido ───────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AccentGreenLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Partido terminado", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccentGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow("Equipo", match.teamName)
                    SummaryRow("Duración", "${match.totalSeconds / 60} min")
                    SummaryRow("Eventos", "${match.events.size}")
                }
            }
        }

        item {
            Button(
                onClick = onShowAllDetails,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver todos los eventos", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        item {
            Text("Jugadores del partido", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Titulares",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AccentGreenLight)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${starters.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGreen
                    )
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
                    onEditPlayerStats = { onEditPlayerStats(player) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Reservas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${substitutes.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
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
                    onEditPlayerStats = { onEditPlayerStats(player) }
                )
            }
        }

        item {
            Text("Línea de tiempo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        }

        if (match.events.isEmpty()) {
            item { Text("No hay eventos registrados", color = TextSecondary) }
        } else {
            items(match.events) { event ->
                MatchEventReportCard(
                    title = formatEventTitle(event.type, event.detail),
                    timeLabel = event.timestampLabel,
                    playerName = event.playerName,
                    detail = event.detail
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
                    .clip(CircleShape)
                    .background(
                        if (role == "Titular") AccentGreenLight else Color(0xFFF5F5F5)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.number.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (role == "Titular") AccentGreen else TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    player.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    "$role · N° ${player.number}",
                    fontSize = 12.sp,
                    color = TextSecondary
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

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = AccentGreen.copy(alpha = 0.7f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccentGreen)
    }
}

@Composable
fun MatchEventReportCard(
    title: String,
    timeLabel: String,
    playerName: String,
    detail: String = ""
) {
    val isSwap = title == "Cambio" || detail.startsWith("Entra ")

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del evento
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = eventIcon(title, detail),
                    contentDescription = title,
                    tint = AccentGreen,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSwap) "Cambio" else title,
                    fontWeight = FontWeight.SemiBold,
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

            // Badge de tiempo
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BgColor)
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(timeLabel.ifBlank { "00:00" }, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
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
        type.startsWith("Asistencia") -> Icons.Default.Send
        type.startsWith("Amarilla") -> Icons.Default.Warning
        type.startsWith("Roja") -> Icons.Default.HighlightOff
        type.startsWith("Disparos al Arco") -> Icons.Default.GpsFixed
        type.startsWith("Ocasiones de Gol") -> Icons.Default.TrackChanges
        type.startsWith("Pelotas Perdidas") -> Icons.Default.Clear
        type.startsWith("Pelotas Recuperadas") -> Icons.Default.Security
        type.startsWith("Centros Buenos") -> Icons.Default.Send
        type.startsWith("Centros Malos") -> Icons.Default.Close
        type.startsWith("Falta a Favor") -> Icons.Default.CheckCircle
        type.startsWith("Falta en Contra") -> Icons.Default.Cancel
        type.startsWith("Corner a Favor") -> Icons.Default.NorthEast
        type.startsWith("Corner en Contra") -> Icons.Default.SouthWest
        type.startsWith("Tiro Libre a Favor") -> Icons.Default.RadioButtonChecked
        type.startsWith("Tiro Libre en Contra") -> Icons.Default.Cancel
        type.startsWith("Tiro Libre Lateral a Favor") -> Icons.Default.ArrowForward
        type.startsWith("Tiro Libre Lateral en Contra") -> Icons.Default.ArrowBack
        else -> Icons.Default.SportsSoccer
    }
}