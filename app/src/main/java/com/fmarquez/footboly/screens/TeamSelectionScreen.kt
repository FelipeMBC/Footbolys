package com.fmarquez.footboly.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fmarquez.footboly.dialog.AddTeamEmojiDialog
import com.fmarquez.footboly.dialog.AddTeamNameDialog
import com.fmarquez.footboly.dialog.AddTeamPlayersDialog
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.vm.FutbolViewModel

private val BackgroundColor  = Color(0xFFF7F7F5)
private val SurfaceColor     = Color(0xFFFFFFFF)
private val AccentGreen      = Color(0xFF1E6B45)
private val AccentGreenLight = Color(0xFFE8F2EC)
private val TextPrimary      = Color(0xFF111111)
private val TextSecondary    = Color(0xFF888888)
private val BorderColor      = Color(0xFFE0E0DC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamSelectionScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTeam = vm.selectedTeam

    var showPlayersDialog by remember { mutableStateOf(false) }
    var showEmojiDialog   by remember { mutableStateOf(false) }
    var showNameDialog    by remember { mutableStateOf(false) }

    var currentPlayerName by remember { mutableStateOf("") }
    val tempPlayers = remember { mutableStateListOf<String>() }

    var tempEmoji    by remember { mutableStateOf("⚽") }
    var tempTeamName by remember { mutableStateOf("") }
    var tempLogoUri  by remember { mutableStateOf<String?>(null) }  // ← nuevo

    fun resetWizard() {
        currentPlayerName = ""
        tempPlayers.clear()
        tempEmoji = "⚽"
        tempTeamName = ""
        tempLogoUri = null               // ← reset
        showPlayersDialog = false
        showEmojiDialog   = false
        showNameDialog    = false
    }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Equipos",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(
                        onClick = { resetWizard(); showPlayersDialog = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar equipo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundColor,
                    scrolledContainerColor = BackgroundColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Selecciona",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Light,
                    color = TextSecondary,
                    fontSize = 28.sp
                )
                Text(
                    text = "tu Equipo",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Dropdown ─────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceColor,
                        contentColor = if (selectedTeam != null) TextPrimary else TextSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        // Miniatura de foto si existe
                        if (selectedTeam?.logoUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(selectedTeam.logoUri).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(24.dp).clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = selectedTeam?.let {
                                if (it.logoUri != null) it.name else "${it.logoEmoji}  ${it.name}"
                            } ?: "Elegir equipo",
                            fontSize = 15.sp,
                            fontWeight = if (selectedTeam != null) FontWeight.Medium else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(text = "▾", color = TextSecondary, fontSize = 14.sp)
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f).background(SurfaceColor)
                ) {
                    vm.teams.forEach { team ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (team.logoUri != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(team.logoUri).crossfade(true).build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(24.dp).clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(team.name, fontSize = 15.sp, color = TextPrimary)
                                    } else {
                                        Text("${team.logoEmoji}  ${team.name}", fontSize = 15.sp, color = TextPrimary)
                                    }
                                }
                            },
                            onClick = { vm.selectTeam(team); expanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Card del equipo seleccionado ──────────────────────────────────
            selectedTeam?.let { team ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentGreenLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Foto o emoji
                        if (team.logoUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(team.logoUri).crossfade(true).build(),
                                contentDescription = "Logo del equipo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, BorderColor, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceColor)
                                    .border(1.dp, BorderColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = team.logoEmoji, fontSize = 42.sp, textAlign = TextAlign.Center)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = team.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            fontSize = 22.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Equipo seleccionado",
                            fontSize = 13.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navHostController.navigate(Screen.PLAYERS_MASTER.route) },
                enabled = selectedTeam != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color.White,
                    disabledContainerColor = BorderColor,
                    disabledContentColor = TextSecondary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(text = "Continuar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    // ── Diálogos ─────────────────────────────────────────────────────────────
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
            onContinue = { showPlayersDialog = false; showEmojiDialog = true }
        )
    }

    if (showEmojiDialog) {
        AddTeamEmojiDialog(
            emojiValue = tempEmoji,
            onEmojiChange = { tempEmoji = it },
            logoUri = tempLogoUri,                   // ← nuevo
            onLogoUriChange = { tempLogoUri = it },  // ← nuevo
            onDismiss = { resetWizard() },
            onSkip = {
                tempEmoji = "⚽"
                tempLogoUri = null
                showEmojiDialog = false
                showNameDialog = true
            },
            onContinue = {
                if (tempEmoji.isBlank() && tempLogoUri == null) tempEmoji = "⚽"
                showEmojiDialog = false
                showNameDialog = true
            }
        )
    }

    if (showNameDialog) {
        AddTeamNameDialog(
            teamName = tempTeamName,
            selectedEmoji = tempEmoji,
            logoUri = tempLogoUri,                   // ← nuevo
            players = tempPlayers,
            onTeamNameChange = { tempTeamName = it },
            onDismiss = { resetWizard() },
            onCreateTeam = {
                vm.addCustomTeam(
                    teamName = tempTeamName,
                    teamEmoji = tempEmoji,
                    playerNames = tempPlayers.toList(),
                    logoUri = tempLogoUri             // ← nuevo
                )
                resetWizard()
            }
        )
    }
}