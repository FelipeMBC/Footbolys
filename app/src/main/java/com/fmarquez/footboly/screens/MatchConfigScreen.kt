package com.fmarquez.footboly.UII.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.vm.FutbolViewModel
import androidx.compose.material.icons.filled.Star
import com.fmarquez.footboly.dialog.MatchSwapDialog


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchConfigScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val team = vm.selectedTeam ?: return
    val match = vm.currentMatch ?: return

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var showIncompleteDialog by remember { mutableStateOf(false) }
    var missingStarters by remember { mutableIntStateOf(0) }
    var missingSubs by remember { mutableIntStateOf(0) }

    var showSwapDialog by remember { mutableStateOf(false) }
    var starterToSwap by remember { mutableStateOf<Player?>(null) }
    var selectedSubstitute by remember { mutableStateOf<Player?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            match.isFinished -> "Partido terminado"
                            match.isStarted -> vm.getFormattedMatchTime()
                            else -> "Nuevo partido - ${team.name}"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (!match.isStarted && !match.isFinished) {
                        FilledIconButton(
                            onClick = {
                                val currentStarters = match.starters.size
                                val currentSubs = match.substitutes.size

                                missingStarters = (11 - currentStarters).coerceAtLeast(0)
                                missingSubs = (5 - currentSubs).coerceAtLeast(0)

                                if (missingStarters > 0 || missingSubs > 0) {
                                    showIncompleteDialog = true
                                } else {
                                    vm.startMatch()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Iniciar partido"
                            )
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
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Titulares (${match.starters.size}/11)") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reservas (${match.substitutes.size}/5)") }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (selectedTab == 0) {
                    Text(
                        text = "Selecciona hasta 11 titulares",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SelectablePlayersList(
                        allPlayers = team.players,
                        selectedPlayers = match.starters,
                        max = 11,
                        blockedPlayers = match.substitutes,
                        enabled = !match.isStarted && !match.isFinished,
                        onToggle = { vm.toggleStarter(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (match.starters.isNotEmpty()) {
                        Text(
                            text = if (match.isStarted) "Titulares en cancha" else "Titulares seleccionados",
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

                                        if (match.isStarted && !match.isFinished) {
                                            IconButton(
                                                onClick = {
                                                    vm.selectPlayerForStats(player.id)
                                                    navHostController.navigate(com.fmarquez.footboly.navigation.Screen.PLAYER_STATS.route)
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
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Send,
                                                    contentDescription = "Cambio"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Selecciona hasta 5 reservas",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SelectablePlayersList(
                        allPlayers = team.players,
                        selectedPlayers = match.substitutes,
                        max = 5,
                        blockedPlayers = match.starters,
                        enabled = !match.isStarted && !match.isFinished,
                        onToggle = { vm.toggleSubstitute(it) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (match.substitutes.isNotEmpty()) {
                        Text(
                            text = "Reservas seleccionadas",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
            }
        }
    }

    if (showIncompleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showIncompleteDialog = false
            },
            title = { Text("Jugadores insuficientes") },

            text = {
                Column {
                    Text("No has completado la cantidad habitual de jugadores.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Titulares seleccionados: ${match.starters.size}/11")
                    Text("Reservas seleccionadas: ${match.substitutes.size}/5")
                    Spacer(modifier = Modifier.height(8.dp))

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
                                }

                                showSwapDialog = false
                                starterToSwap = null
                                selectedSubstitute = null
                            }
                        )
                    }

                    if (missingStarters > 0) {
                        Text("Faltan $missingStarters titulares")
                    }

                    if (missingSubs > 0) {
                        Text("Faltan $missingSubs reservas")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showIncompleteDialog = false
                    }
                ) {
                    Text("Volver a seleccionar")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showIncompleteDialog = false
                        vm.startMatch()
                    }
                ) {
                    Text("Continuar de todas formas")
                }
            }
        )
    }
}

@Composable
fun SelectablePlayersList(
    allPlayers: List<Player>,
    selectedPlayers: List<Player>,
    max: Int,
    blockedPlayers: List<Player> = emptyList(),
    enabled: Boolean = true,
    onToggle: (Player) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(allPlayers, key = { it.id }) { player ->
            val isSelected = selectedPlayers.any { it.id == player.id }
            val isBlocked = blockedPlayers.any { it.id == player.id }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = enabled && !isBlocked) {
                        if (isSelected || selectedPlayers.size < max) {
                            onToggle(player)
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        !enabled -> MaterialTheme.colorScheme.surfaceVariant
                        isBlocked -> Color.LightGray
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(player.name, fontWeight = FontWeight.Bold)
                        Text("N° ${player.number}")
                    }

                    Text(
                        when {
                            !enabled -> "Bloqueado"
                            isBlocked -> "Ya está en la otra lista"
                            isSelected -> "Seleccionado"
                            else -> "Tocar"
                        }
                    )
                }
            }
        }
    }
}