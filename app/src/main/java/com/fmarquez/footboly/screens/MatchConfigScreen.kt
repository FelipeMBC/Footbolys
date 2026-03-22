package com.fmarquez.footboly.screens

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.vm.FutbolViewModel

// ── Paleta compartida ────────────────────────────────────────────────────────
private val BgColor         = Color(0xFFF7F7F5)
private val SurfaceColor    = Color(0xFFFFFFFF)
private val AccentGreen     = Color(0xFF1E6B45)
private val AccentGreenLight= Color(0xFFE8F2EC)
private val TextPrimary     = Color(0xFF111111)
private val TextSecondary   = Color(0xFF888888)
private val BorderColor     = Color(0xFFE0E0DC)
private val ErrorRed        = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchConfigScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val team = vm.selectedTeam ?: return
    val match = vm.currentMatch ?: return
    val context = LocalContext.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showIncompleteDialog by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var missingStarters by remember { mutableIntStateOf(0) }
    var missingSubs by remember { mutableIntStateOf(0) }
    var matchDurationMinutes by remember { mutableFloatStateOf(60f) }

    LaunchedEffect(match.isStarted, match.isFinished) {
        if (match.isStarted && !match.isFinished) {
            navHostController.navigate(Screen.REPORT_SCREEN.route) {
                popUpTo(Screen.MATCH_CONFIG.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun openDurationDialog() {
        matchDurationMinutes = ((match.totalSeconds / 60).coerceIn(10, 90)).toFloat()
        showDurationDialog = true
    }

    fun startMatchWithSelectedDuration() {
        vm.setMatchDuration(matchDurationMinutes.toInt())
        vm.startMatch()
        navHostController.navigate(Screen.REPORT_SCREEN.route) {
            popUpTo(Screen.MATCH_CONFIG.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            match.isFinished -> "Partido terminado"
                            match.isStarted  -> vm.getFormattedMatchTime()
                            else             -> team.name
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (match.isStarted && !match.isFinished) {
                                Toast.makeText(context, "No puedes volver mientras el partido está en curso", Toast.LENGTH_SHORT).show()
                            } else {
                                navHostController.popBackStack()
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                },
                actions = {
                    if (!match.isStarted && !match.isFinished) {
                        IconButton(
                            onClick = {
                                val currentStarters = match.starters.size
                                val currentSubs = match.substitutes.size
                                val totalSeleccionados = currentStarters + currentSubs

                                if (totalSeleccionados < 5) {
                                    Toast.makeText(context, "Selecciona al menos 5 jugadores para iniciar", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }

                                missingStarters = (11 - currentStarters).coerceAtLeast(0)
                                missingSubs = (5 - currentSubs).coerceAtLeast(0)

                                if (missingStarters > 0 || missingSubs > 0) showIncompleteDialog = true
                                else openDurationDialog()
                            },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar partido", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Tabs ─────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceColor,
                contentColor = AccentGreen,
                indicator = { tabPositions ->
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(2.dp)
                            .background(AccentGreen)
                    )
                },
                divider = { Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(BorderColor)) }
            ) {
                listOf(
                    "Titulares (${match.starters.size}/11)",
                    "Reservas (${match.substitutes.size}/5)"
                ).forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedTab == i) AccentGreen else TextSecondary
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                val label = if (selectedTab == 0) "Selecciona hasta 11 titulares" else "Selecciona hasta 5 reservas"

                Text(
                    text = label,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    SelectablePlayersList(
                        allPlayers = team.players,
                        selectedPlayers = match.starters,
                        max = 11,
                        blockedPlayers = match.substitutes,
                        enabled = !match.isStarted && !match.isFinished,
                        onToggle = { vm.toggleStarter(it) }
                    )
                } else {
                    SelectablePlayersList(
                        allPlayers = team.players,
                        selectedPlayers = match.substitutes,
                        max = 5,
                        blockedPlayers = match.starters,
                        enabled = !match.isStarted && !match.isFinished,
                        onToggle = { vm.toggleSubstitute(it) }
                    )
                }
            }
        }
    }

    // ── Diálogo: jugadores insuficientes ─────────────────────────────────────
    if (showIncompleteDialog) {
        AlertDialog(
            onDismissRequest = { showIncompleteDialog = false },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Equipo incompleto", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text("No completaste la cantidad habitual de jugadores.", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatRow("Titulares", "${match.starters.size}/11")
                    StatRow("Reservas", "${match.substitutes.size}/5")
                    if (missingStarters > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Faltan $missingStarters titulares", color = ErrorRed, fontSize = 13.sp)
                    }
                    if (missingSubs > 0) {
                        Text("Faltan $missingSubs reservas", color = ErrorRed, fontSize = 13.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showIncompleteDialog = false }) {
                    Text("Volver", color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val totalSeleccionados = match.starters.size + match.substitutes.size
                        if (totalSeleccionados < 5) {
                            Toast.makeText(context, "Selecciona al menos 5 jugadores", Toast.LENGTH_SHORT).show()
                            showIncompleteDialog = false
                            return@TextButton
                        }
                        showIncompleteDialog = false
                        openDurationDialog()
                    }
                ) {
                    Text("Continuar de todas formas", color = AccentGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // ── Diálogo: duración del partido ─────────────────────────────────────────
    if (showDurationDialog) {
        AlertDialog(
            onDismissRequest = { showDurationDialog = false },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Duración del partido", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text("Entre 10 y 90 minutos", color = TextSecondary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentGreenLight)
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${matchDurationMinutes.toInt()} min",
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = AccentGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = matchDurationMinutes,
                        onValueChange = { matchDurationMinutes = it },
                        valueRange = 10f..90f,
                        steps = 79,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentGreen,
                            activeTrackColor = AccentGreen,
                            inactiveTrackColor = BorderColor
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDurationDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDurationDialog = false
                        startMatchWithSelectedDuration()
                    }
                ) {
                    Text("Iniciar", color = AccentGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
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

            val cardBg = when {
                !enabled   -> Color(0xFFF0F0EE)
                isBlocked  -> Color(0xFFF5F5F5)
                isSelected -> AccentGreenLight
                else       -> SurfaceColor
            }

            val borderCol = when {
                isSelected -> AccentGreen.copy(alpha = 0.4f)
                else       -> BorderColor
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled && !isBlocked) {
                        if (isSelected || selectedPlayers.size < max) onToggle(player)
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Número del jugador en pastilla
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) AccentGreen else BorderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = player.number.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = player.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (isBlocked) TextSecondary else TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = when {
                            !enabled   -> "Bloqueado"
                            isBlocked  -> "En otra lista"
                            isSelected -> "✓"
                            else       -> ""
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isSelected -> AccentGreen
                            else       -> TextSecondary
                        }
                    )
                }
            }
        }
    }
}