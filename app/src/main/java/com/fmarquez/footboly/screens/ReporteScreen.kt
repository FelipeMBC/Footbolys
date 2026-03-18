package com.fmarquez.footboly.screens

import com.fmarquez.footboly.dialog.MatchSwapDialog
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.dialog.MatchSwapDialog
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.vm.FutbolViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val context = LocalContext.current
    val team = vm.selectedTeam ?: return
    val match = vm.currentMatch ?: return

    var showSwapDialog by remember { mutableStateOf(false) }
    var starterToSwap by remember { mutableStateOf<Player?>(null) }
    var selectedSubstitute by remember { mutableStateOf<Player?>(null) }
    var showStopMatchDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (match.isFinished) "Partido terminado"
                        else vm.getFormattedMatchTime()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (!match.isFinished) {
                        TextButton(
                            onClick = {
                                showStopMatchDialog = true
                            }
                        ) {
                            Text("Detener partido")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Reporte - ${team.name}",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Titulares en cancha",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(match.starters, key = { it.id }) { player ->
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(player.name, fontWeight = FontWeight.Bold)
                                Text("Titular · N° ${player.number}")
                            }

                            IconButton(
                                onClick = {
                                    vm.selectPlayerForStats(player.id)
                                    navHostController.navigate(Screen.PLAYER_STATS.route)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Estadística"
                                )
                            }

                            IconButton(
                                onClick = {
                                    starterToSwap = player
                                    selectedSubstitute = null
                                    showSwapDialog = true
                                },
                                enabled = !match.isFinished && match.substitutes.isNotEmpty()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Cambio"
                                )
                            }
                            if (showStopMatchDialog) {
                                AlertDialog(
                                    onDismissRequest = {
                                        showStopMatchDialog = false
                                    },
                                    title = { Text("Finalizar partido") },
                                    text = {
                                        Text("¿Está seguro de finalizar el partido?")
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                showStopMatchDialog = false
                                            }
                                        ) {
                                            Text("No")
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                vm.stopMatch()
                                                showStopMatchDialog = false
                                            }
                                        ) {
                                            Text("Sí")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Reservas disponibles",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(match.substitutes, key = { it.id }) { player ->
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(player.name, fontWeight = FontWeight.Bold)
                                Text("Reserva · N° ${player.number}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSwapDialog && starterToSwap != null) {
        MatchSwapDialog(
            starter = starterToSwap!!,
            substitutes = match.substitutes,
            selectedSubstitute = selectedSubstitute,
            onSelectSubstitute = { selectedSubstitute = it },
            onDismiss = {
                showSwapDialog = false
                starterToSwap = null
                selectedSubstitute = null
            },
            onConfirm = {
                val starter = starterToSwap
                val substitute = selectedSubstitute

                if (starter != null && substitute != null) {
                    vm.swapPlayerDuringMatch(starter, substitute)

                    Toast.makeText(
                        context,
                        "Jugador cambiado, tiempo ${vm.getFormattedMatchTime()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                showSwapDialog = false
                starterToSwap = null
                selectedSubstitute = null
            }
        )
    }
}