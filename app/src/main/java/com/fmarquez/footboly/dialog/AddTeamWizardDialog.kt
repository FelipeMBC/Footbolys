package com.fmarquez.footboly.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddTeamPlayersDialog(
    currentPlayerName: String,
    players: List<String>,
    onPlayerNameChange: (String) -> Unit,
    onAddPlayer: () -> Unit,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar jugadores") },
        text = {
            Column {
                Text("Ingresa entre 11 y 30 jugadores")
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = currentPlayerName,
                    onValueChange = onPlayerNameChange,
                    label = { Text("Nombre del jugador") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onAddPlayer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Agregar jugador")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Jugadores agregados: ${players.size} /30")

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    itemsIndexed(players) { index, player ->
                        Text(
                            text = "${index + 1}. $player",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onContinue,
                enabled = players.size >= 11
            ) {
                Text("Continuar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AddTeamEmojiDialog(
    emojiValue: String,
    onEmojiChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emoji del equipo") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Puedes ingresar un emoji para identificar el equipo")
                OutlinedTextField(
                    value = emojiValue,
                    onValueChange = onEmojiChange,
                    label = { Text("Emoji del equipo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text("Continuar")
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onSkip) {
                    Text("Omitir")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
fun AddTeamNameDialog(
    teamName: String,
    selectedEmoji: String,
    players: List<String>,
    onTeamNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreateTeam: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nombre del equipo") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Emoji: $selectedEmoji")
                Text("Jugadores cargados: ${players.size}")

                OutlinedTextField(
                    value = teamName,
                    onValueChange = onTeamNameChange,
                    label = { Text("Nombre del equipo") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCreateTeam,
                enabled = teamName.isNotBlank()
            ) {
                Text("Crear equipo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}