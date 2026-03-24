package com.fmarquez.footboly.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.fmarquez.footboly.dialog.MatchSwapDialog
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.vm.FutbolViewModel

private val BgColor          = Color(0xFFF7F7F5)
private val SurfaceColor     = Color(0xFFFFFFFF)
private val AccentGreen      = Color(0xFF1E6B45)
private val AccentGreenLight = Color(0xFFE8F2EC)
private val TextPrimary      = Color(0xFF111111)
private val TextSecondary    = Color(0xFF888888)
private val BorderColor      = Color(0xFFE0E0DC)
private val ErrorRed         = Color(0xFFD32F2F)

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

    BackHandler(enabled = !match.isFinished) {
        Toast.makeText(context, "Debes finalizar el partido para salir del reporte", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(match.isFinished) {
        if (match.isFinished) {
            vm.selectFinishedMatch(match)
            navHostController.navigate(Screen.MATCH_TIMELINE.route) {
                popUpTo(navHostController.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
                restoreState = false
            }
        }
    }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (match.isFinished) "Partido terminado" else vm.getFormattedMatchTime(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = team.name,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!match.isFinished) {
                                Toast.makeText(context, "Debes finalizar el partido para salir", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextSecondary)
                    }
                },
                actions = {
                    if (!match.isFinished) {
                        TextButton(
                            onClick = { showStopMatchDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                        ) {
                            Text("Finalizar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Titulares header ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Titulares", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AccentGreenLight)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text("${match.starters.size}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentGreen)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Titulares card — weight(1f) para scrollear dentro de la caja ─
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)                // ← altura proporcional, scrolleable
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(match.starters, key = { it.id }) { player ->
                        PlayerRowItem(
                            player = player,
                            role = "Titular",
                            showStats = true,
                            showSwap = !match.isFinished && match.substitutes.isNotEmpty(),
                            onStats = {
                                vm.selectPlayerForStats(player.id)
                                navHostController.navigate(Screen.PLAYER_STATS.route)
                            },
                            onSwap = {
                                starterToSwap = player
                                selectedSubstitute = null
                                showSwapDialog = true
                            }
                        )
                        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Reservas header ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reservas", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text("${match.substitutes.size}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Reservas card — weight(0.6f) para ocupar menos que titulares ─
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)              // ← altura proporcional, scrolleable
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (match.substitutes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sin reservas disponibles", color = TextSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(horizontal = 4.dp)) {
                        items(match.substitutes, key = { it.id }) { player ->
                            PlayerRowItem(player = player, role = "Reserva")
                            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // ── Diálogo: finalizar partido ────────────────────────────────────────────
    if (showStopMatchDialog) {
        AlertDialog(
            onDismissRequest = { showStopMatchDialog = false },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Finalizar partido", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("¿Estás seguro de finalizar el partido?", color = TextSecondary) },
            dismissButton = {
                TextButton(onClick = { showStopMatchDialog = false }) { Text("Cancelar", color = TextSecondary) }
            },
            confirmButton = {
                TextButton(onClick = { vm.stopMatch(); showStopMatchDialog = false }) {
                    Text("Finalizar", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // ── Diálogo: cambio de jugador ────────────────────────────────────────────
    if (showSwapDialog && starterToSwap != null) {
        MatchSwapDialog(
            starter = starterToSwap!!,
            substitutes = match.substitutes,
            selectedSubstitute = selectedSubstitute,
            onSelectSubstitute = { selectedSubstitute = it },
            onDismiss = { showSwapDialog = false; starterToSwap = null; selectedSubstitute = null },
            onConfirm = {
                val starter = starterToSwap
                val substitute = selectedSubstitute
                if (starter != null && substitute != null) {
                    vm.registerSwap(starter = starter, sub = substitute)
                    Toast.makeText(context, "Cambio registrado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Selecciona una reserva", Toast.LENGTH_SHORT).show()
                    return@MatchSwapDialog
                }
                showSwapDialog = false; starterToSwap = null; selectedSubstitute = null
            }
        )
    }
}

@Composable
private fun PlayerRowItem(
    player: Player,
    role: String,
    showStats: Boolean = false,
    showSwap: Boolean = false,
    onStats: () -> Unit = {},
    onSwap: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (role == "Titular") AccentGreenLight else Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.number.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (role == "Titular") AccentGreen else TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(player.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
            Text("$role · N° ${player.number}", fontSize = 12.sp, color = TextSecondary)
        }

        if (showStats) {
            IconButton(onClick = onStats, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.StackedLineChart, contentDescription = "Estadísticas", tint = AccentGreen, modifier = Modifier.size(18.dp))
            }
        }

        if (showSwap) {
            IconButton(onClick = onSwap, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Downloading, contentDescription = "Cambio", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}