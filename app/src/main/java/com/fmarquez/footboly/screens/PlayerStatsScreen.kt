package com.fmarquez.footboly.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.vm.FutbolViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerStatsScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val player = vm.getSelectedPlayer() ?: return
    val stats = vm.getPlayerStats(player.id)

    var minute by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas - ${player.name}") },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = minute,
                    onValueChange = { minute = it },
                    label = { Text("Minuto del evento") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                StatButtonRow(
                    title = "Tarjetas amarillas",
                    value = stats.yellowCards,
                    onAdd = {
                        stats.yellowCards++
                        vm.addEvent(minute, "Tarjeta Amarilla", player.name)
                    },
                    onRemove = {
                        if (stats.yellowCards > 0) stats.yellowCards--
                    }
                )
            }

            item {
                StatButtonRow(
                    title = "Tarjetas rojas",
                    value = stats.redCards,
                    onAdd = {
                        stats.redCards++
                        vm.addEvent(minute, "Tarjeta Roja", player.name)
                    },
                    onRemove = {
                        if (stats.redCards > 0) stats.redCards--
                    }
                )
            }

            item {
                StatButtonRow(
                    title = "Goles",
                    value = stats.goals,
                    onAdd = {
                        stats.goals++
                        vm.addEvent(minute, "Gol", player.name)
                    },
                    onRemove = {
                        if (stats.goals > 0) stats.goals--
                    }
                )
            }

            item {
                StatButtonRow(
                    title = "Asistencias",
                    value = stats.assists,
                    onAdd = {
                        stats.assists++
                        vm.addEvent(minute, "Asistencia", player.name)
                    },
                    onRemove = {
                        if (stats.assists > 0) stats.assists--
                    }
                )
            }

            item {
                StatButtonRow(
                    title = "Corners",
                    value = stats.corners,
                    onAdd = {
                        stats.corners++
                        vm.addEvent(minute, "Corner", player.name)
                    },
                    onRemove = {
                        if (stats.corners > 0) stats.corners--
                    }
                )
            }

            item {
                StatButtonRow(
                    title = "Tiros libres a favor",
                    value = stats.freeKicks,
                    onAdd = {
                        stats.freeKicks++
                        vm.addEvent(minute, "Tiro Libre", player.name)
                    },
                    onRemove = {
                        if (stats.freeKicks > 0) stats.freeKicks--
                    }
                )
            }

            item {
                StatButtonRow(
                    title = "Pelotas recuperadas",
                    value = stats.recoveries,
                    onAdd = {
                        stats.recoveries++
                        vm.addEvent(minute, "Pelota Recuperada", player.name)
                    },
                    onRemove = {
                        if (stats.recoveries > 0) stats.recoveries--
                    }
                )
            }
        }
    }
}

@Composable
fun StatButtonRow(
    title: String,
    value: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text("Valor actual: $value")
            }

            OutlinedButton(onClick = onRemove) {
                Text("-")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = onAdd) {
                Text("+")
            }
        }
    }
}