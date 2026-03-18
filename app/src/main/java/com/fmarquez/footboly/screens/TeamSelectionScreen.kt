package com.fmarquez.footboly.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.dialog.AddTeamEmojiDialog
import com.fmarquez.footboly.dialog.AddTeamNameDialog
import com.fmarquez.footboly.dialog.AddTeamPlayersDialog
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.vm.FutbolViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamSelectionScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTeam = vm.selectedTeam

    var showPlayersDialog by remember { mutableStateOf(false) }
    var showEmojiDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    var currentPlayerName by remember { mutableStateOf("") }
    val tempPlayers = remember { mutableStateListOf<String>() }

    var tempEmoji by remember { mutableStateOf("⚽") }
    var tempTeamName by remember { mutableStateOf("") }

    fun resetWizard() {
        currentPlayerName = ""
        tempPlayers.clear()
        tempEmoji = "⚽"
        tempTeamName = ""
        showPlayersDialog = false
        showEmojiDialog = false
        showNameDialog = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selección de equipo") },
                actions = {
                    IconButton(
                        onClick = {
                            resetWizard()
                            showPlayersDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar equipo"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Selecciona un equipo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedTeam?.name ?: "Elegir equipo")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    vm.teams.forEach { team ->
                        DropdownMenuItem(
                            text = { Text("${team.logoEmoji} ${team.name}") },
                            onClick = {
                                vm.selectTeam(team)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            selectedTeam?.let { team ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = team.logoEmoji,
                            fontSize = 72.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = team.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    navHostController.navigate(Screen.PLAYERS_MASTER.route)
                },
                enabled = selectedTeam != null,
                modifier = Modifier.width(220.dp)
            ) {
                Text("Continuar")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showPlayersDialog) {
        AddTeamPlayersDialog(
            currentPlayerName = currentPlayerName,
            players = tempPlayers,
            onPlayerNameChange = { currentPlayerName = it },
            onAddPlayer = {
                val trimmed = currentPlayerName.trim()
                if (trimmed.isNotBlank() && tempPlayers.size < 30) {
                    tempPlayers.add(trimmed)
                    currentPlayerName = ""

                    if (tempPlayers.size == 30) {
                        showPlayersDialog = false
                        showEmojiDialog = true
                    }
                }
            },
            onDismiss = { resetWizard() },
            onContinue = {
                showPlayersDialog = false
                showEmojiDialog = true
            }
        )
    }

    if (showEmojiDialog) {
        AddTeamEmojiDialog(
            emojiValue = tempEmoji,
            onEmojiChange = { tempEmoji = it },
            onDismiss = { resetWizard() },
            onSkip = {
                tempEmoji = "⚽"
                showEmojiDialog = false
                showNameDialog = true
            },
            onContinue = {
                if (tempEmoji.isBlank()) {
                    tempEmoji = "⚽"
                }
                showEmojiDialog = false
                showNameDialog = true
            }
        )
    }

    if (showNameDialog) {
        AddTeamNameDialog(
            teamName = tempTeamName,
            selectedEmoji = tempEmoji,
            players = tempPlayers,
            onTeamNameChange = { tempTeamName = it },
            onDismiss = { resetWizard() },
            onCreateTeam = {
                vm.addCustomTeam(
                    teamName = tempTeamName,
                    teamEmoji = tempEmoji,
                    playerNames = tempPlayers.toList()
                )
                resetWizard()
            }
        )
    }
}