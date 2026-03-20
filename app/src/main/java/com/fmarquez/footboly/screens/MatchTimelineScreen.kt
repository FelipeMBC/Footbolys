package com.fmarquez.footboly.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Card
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.modelos.MatchRecord
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.vm.FutbolViewModel

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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedMatch == null) "Ver Partidos"
                        else "Detalle del partido"
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (selectedMatch != null) {
                                vm.clearSelectedFinishedMatch()
                            } else {
                                navHostController.popBackStack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (selectedMatch == null) {
            MatchHistoryList(
                matches = vm.finishedMatches,
                onSelectMatch = { vm.selectFinishedMatch(it) },
                onEditMatch = { vm.selectFinishedMatch(it) },
                onDeleteMatch = { matchToDelete = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            )
        } else {
            MatchDetailContent(
                match = selectedMatch,
                onShowAllDetails = { showAllDetailsDialog = true },
                onEditPlayerStats = { player ->
                    vm.startEditingPlayerFromFinishedMatch(selectedMatch, player.id)
                    navHostController.navigate(Screen.PLAYER_STATS.route)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            )
        }
    }

    if (showAllDetailsDialog && selectedMatch != null) {
        AlertDialog(
            onDismissRequest = { showAllDetailsDialog = false },
            title = { Text("Todos los eventos") },
            text = {
                Column {
                    if (selectedMatch.events.isEmpty()) {
                        Text("No hay eventos registrados")
                    } else {
                        selectedMatch.events.forEach { event ->
                            Text(
                                "${formatEventTitle(event.type, event.detail)} - ${event.playerName} - ${event.timestampLabel}"
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllDetailsDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (matchToDelete != null) {
        AlertDialog(
            onDismissRequest = { matchToDelete = null },
            title = { Text("Eliminar partido") },
            text = { Text("¿Está seguro de eliminar este partido guardado?") },
            dismissButton = {
                TextButton(onClick = { matchToDelete = null }) {
                    Text("No")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteFinishedMatch(matchToDelete!!.id)
                        matchToDelete = null
                    }
                ) {
                    Text("Sí")
                }
            }
        )
    }

    if (vm.shouldShowEditResultDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissEditResultDialog() },
            title = { Text("Cambios realizados") },
            text = {
                Column {
                    if (vm.lastEditChanges.isEmpty()) {
                        Text("No hubo cambios")
                    } else {
                        vm.lastEditChanges.forEach { line ->
                            Text(line)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.dismissEditResultDialog() }) {
                    Text("Aceptar")
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
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Aún no hay partidos guardados")
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(matches, key = { it.id }) { match ->
                var expanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMatch(match) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Partido: ${match.teamName}",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(match.finishedAtLabel.ifBlank { "Sin fecha" })
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("Duración: ${match.totalSeconds / 60}:00")
                            Text("Eventos: ${match.events.size}")
                        }

                        Box {
                            IconButton(onClick = { expanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opciones"
                                )
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Editar") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                    },
                                    onClick = {
                                        expanded = false
                                        onEditMatch(match)
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Eliminar") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null)
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
    onShowAllDetails: () -> Unit,
    onEditPlayerStats: (Player) -> Unit,
    modifier: Modifier = Modifier
) {
    val matchPlayers = (match.starters + match.substitutes)
        .distinctBy { it.id }
        .sortedBy { it.number }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Partido terminado",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Equipo: ${match.teamName}")
                    Text("Duración: ${match.totalSeconds / 60}:00")
                    Text("Eventos: ${match.events.size}")
                }
            }
        }

        item {
            Button(
                onClick = onShowAllDetails,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ver todos los detalles")
            }
        }

        item {
            Text(
                text = "Editar estadísticas por jugador",
                fontWeight = FontWeight.Bold
            )
        }

        if (matchPlayers.isEmpty()) {
            item {
                Text("No hay jugadores registrados en este partido")
            }
        } else {
            items(matchPlayers, key = { it.id }) { player ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.name,
                                fontWeight = FontWeight.Bold
                            )
                            Text("N° ${player.number}")
                        }

                        OutlinedButton(
                            onClick = { onEditPlayerStats(player) }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Editar")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Reporte",
                fontWeight = FontWeight.Bold
            )
        }

        if (match.events.isEmpty()) {
            item {
                Text("No hay eventos registrados")
            }
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

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isSwap) "Cambio" else title,
                    fontWeight = FontWeight.Bold
                )

                if (isSwap) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(detail.ifBlank { "Cambio de jugador" })
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(timeLabel.ifBlank { "00:00" })
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(playerName.ifBlank { "Sin jugador" })
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Card {
                Box(
                    modifier = Modifier.padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = eventIcon(title, detail),
                        contentDescription = title
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