package com.fmarquez.footboly.screens

import android.widget.Toast
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.fmarquez.footboly.dialog.AddPlayerDialog
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.vm.FutbolViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersMasterScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val team = vm.selectedTeam ?: return
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var playerName by remember { mutableStateOf("") }
    var playerNumber by remember { mutableStateOf("") }

    var playerToDelete by remember { mutableStateOf<Player?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var showMinPlayersWarning by remember { mutableStateOf(false) }

    LaunchedEffect(team.players.size) {
        if (team.players.size < 11) {
            showMinPlayersWarning = true
        } else {
            showMinPlayersWarning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${team.logoEmoji} ${team.name}") },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
                text = "Maestro de jugadores",
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (team.players.size < 30) {
                            showAddDialog = true
                        } else {
                            Toast.makeText(
                                context,
                                "Máximo 30 jugadores por equipo",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Agregar jugador")
                }

                Button(
                    onClick = {
                        if (team.players.size < 11) {
                            showMinPlayersWarning = true
                        } else {
                            vm.createNewMatch()
                            navHostController.navigate(Screen.MATCH_CONFIG.route)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Nuevo partido")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        navHostController.navigate(Screen.MATCH_TIMELINE.route)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ver partido")
                }

                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reporte")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    items(team.players, key = { it.id }) { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(player.name, fontWeight = FontWeight.Bold)
                                Text("N° ${player.number}")
                            }

                            IconButton(
                                onClick = {
                                    playerToDelete = player
                                    showDeleteDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                            }
                        }
                        Divider()
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlayerDialog(
            playerName = playerName,
            onNameChange = { playerName = it },
            onDismiss = {
                showAddDialog = false
                if (team.players.size < 11) {
                    showMinPlayersWarning = true
                }
            },
            onConfirm = {
                if (team.players.size >= 30) {
                    Toast.makeText(
                        context,
                        "Máximo 30 jugadores por equipo",
                        Toast.LENGTH_SHORT
                    ).show()
                    showAddDialog = false
                    return@AddPlayerDialog
                }

                vm.addPlayer(playerName)
                playerName = ""
                showAddDialog = false

                if (team.players.size < 11) {
                    showMinPlayersWarning = true
                } else {
                    showMinPlayersWarning = false
                }
            }
        )
    }

    if (showDeleteDialog && playerToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                playerToDelete = null
            },
            title = { Text("Confirmar eliminación") },
            text = {
                Text("¿Está seguro de eliminar al jugador ${playerToDelete?.name}?")
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        playerToDelete = null
                    }
                ) {
                    Text("Atrás")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playerToDelete?.let { player ->
                            vm.removePlayer(player)
                            Toast.makeText(
                                context,
                                "Jugador eliminado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showDeleteDialog = false
                        playerToDelete = null

                        if ((vm.selectedTeam?.players?.size ?: 0) < 11) {
                            showMinPlayersWarning = true
                        }
                    }
                ) {
                    Text("Eliminar")
                }
            }
        )
    }

    if (showMinPlayersWarning) {
        AlertDialog(
            onDismissRequest = {
                showMinPlayersWarning = false
                showAddDialog = true
            },
            title = { Text("Equipo incompleto") },
            text = {
                Text("Hay menos de 11 jugadores en este equipo, por favor agregar.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMinPlayersWarning = false
                        showAddDialog = true
                    }
                ) {
                    Text("Agregar jugador")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMinPlayersWarning = false
                        showAddDialog = true
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}