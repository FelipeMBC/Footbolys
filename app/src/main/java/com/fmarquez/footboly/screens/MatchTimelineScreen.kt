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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.modelos.MatchRecord
import com.fmarquez.footboly.vm.FutbolViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchTimelineScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val selectedMatch = vm.selectedFinishedMatch
    var showAllDetailsDialog by remember { mutableStateOf(false) }

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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            )
        } else {
            MatchDetailContent(
                match = selectedMatch,
                onShowAllDetails = { showAllDetailsDialog = true },
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
                            Text("${event.type} - ${event.playerName} - ${event.timestampLabel}")
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
}

@Composable
fun MatchHistoryList(
    matches: List<MatchRecord>,
    onSelectMatch: (MatchRecord) -> Unit,
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

                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones"
                        )
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onShowAllDetails,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Star, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Ver todos los detalles")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Reporte",
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (match.events.isEmpty()) {
            Text("No hay eventos registrados")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(match.events) { event ->
                    MatchEventReportCard(
                        title = event.type,
                        timeLabel = event.timestampLabel,
                        playerName = event.playerName
                    )
                }
            }
        }
    }
}

@Composable
fun MatchEventReportCard(
    title: String,
    timeLabel: String,
    playerName: String
) {
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
                    text = title,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
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
                    Text(playerName)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Card {
                Box(
                    modifier = Modifier.padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = eventIcon(title),
                        contentDescription = title
                    )
                }
            }
        }
    }
}

fun eventIcon(type: String): ImageVector {
    return when (type) {
        "Gol" -> Icons.Default.Build
        "Asistencia" -> Icons.Default.Send
        "Amarilla" -> Icons.Default.Warning
        "Roja" -> Icons.Default.Build
        "Disparos al Arco" -> Icons.Default.Build
        "Ocasiones de Gol" -> Icons.Default.Build
        "Pelotas Perdidas" -> Icons.Default.Clear
        "Pelotas Recuperadas" -> Icons.Default.Build
        "Centros Buenos" -> Icons.Default.Send
        "Centros Malos" -> Icons.Default.Build
        "Falta a Favor" -> Icons.Default.CheckCircle
        "Falta en Contra" -> Icons.Default.Close
        "Corner a Favor" -> Icons.Default.CheckCircle
        "Corner en Contra" -> Icons.Default.Close
        "Tiro Libre a Favor" -> Icons.Default.CheckCircle
        "Tiro Libre en Contra" -> Icons.Default.Close
        "Tiro Libre Lateral a Favor" -> Icons.Default.CheckCircle
        "Tiro Libre Lateral en Contra" -> Icons.Default.Close
        else -> Icons.Default.Build
    }
}