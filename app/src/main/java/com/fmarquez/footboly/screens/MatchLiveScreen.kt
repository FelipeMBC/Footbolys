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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.fmarquez.footboly.dialog.MatchSwapDialog
import com.fmarquez.footboly.modelos.MatchEvent
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.util.hexToColor
import com.fmarquez.footboly.util.teamColorLight
import com.fmarquez.footboly.vm.FutbolViewModel
import kotlinx.coroutines.delay

private val BgColor = Color(0xFFF7F7F5)
private val SurfaceColor = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF111111)
private val TextSecondary = Color(0xFF888888)
private val BorderColor = Color(0xFFE0E0DC)
private val ErrorRed = Color(0xFFD32F2F)
private val ErrorRedLight = Color(0xFFFFF1F1)
private val InjuryAmber = Color(0xFFE65100)
private val InjuryLight = Color(0xFFFFF3E0)
private val BlockedGray = Color(0xFFEAEAEA)
private val BlockedText = Color(0xFF8C8C8C)
private val YellowCardColor = Color(0xFFF2B705)

private fun parseEventCount(detail: String): Int {
    return detail.substringAfter(": ", "").toIntOrNull() ?: 1
}

private fun ownGoals(events: List<MatchEvent>): Int {
    return events
        .filter { it.type == "Gol a Favor" }
        .sumOf { parseEventCount(it.detail) }
}

private fun playerCardCounts(
    playerId: Int,
    events: List<MatchEvent>
): Pair<Int, Int> {
    val yellow = events
        .filter { it.playerId == playerId && it.type == "Amarilla" }
        .sumOf { parseEventCount(it.detail) }
        .coerceAtMost(2)

    val red = events
        .filter { it.playerId == playerId && it.type == "Roja" }
        .sumOf { parseEventCount(it.detail) }
        .coerceAtMost(1)

    return yellow to red
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchLiveScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val match = vm.currentMatch ?: return
    val team = vm.selectedTeam ?: return

    val displayedStarters = vm.getCurrentStarters(match)
    val displayedSubstitutes = vm.getCurrentSubstitutes(match)
    val expelledPlayers = match.expelledPlayers.sortedBy { it.number }
    val injuredPlayers = match.injuredPlayers.sortedBy { it.number }

    val teamColor = hexToColor(team.shirtColorHex)
    val teamColorLight = teamColorLight(teamColor)

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showSwapDialog by remember { mutableStateOf(false) }
    var starterToSwap by remember { mutableStateOf<Player?>(null) }
    var selectedSubstitute by remember { mutableStateOf<Player?>(null) }
    var showStopMatchDialog by remember { mutableStateOf(false) }
    var showOpponentEventDialog by remember { mutableStateOf(false) }

    val opponentGoals = match.opponentGoals
    val opponentGoalChances = match.opponentGoalChances

    produceState(initialValue = 0, key1 = match.isStarted, key2 = match.isFinished) {
        while (match.isStarted && !match.isFinished) {
            delay(1000)
            value++
        }
    }

    val currentOwnGoals = ownGoals(match.events)
    val rivalName = match.rivalName.ifBlank { "Equipo Rival" }

    BackHandler(enabled = !match.isFinished) {
        Toast.makeText(
            navHostController.context,
            "Debes finalizar el partido para salir",
            Toast.LENGTH_SHORT
        ).show()
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
                    Text(
                        text = vm.getFormattedMatchTime(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            Toast.makeText(
                                navHostController.context,
                                "Debes finalizar el partido para salir",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextSecondary
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showStopMatchDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Text("Finalizar", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
            Spacer(modifier = Modifier.height(6.dp))

            MatchScoreHeader(
                teamName = team.name,
                rivalName = rivalName,
                ownGoals = currentOwnGoals,
                opponentGoals = opponentGoals,
                onOpponentPlus = { showOpponentEventDialog = true },
                onOpponentMinus = { vm.updateOpponentGoals(-1) },
                teamColor = teamColor,
                teamColorLight = teamColorLight
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Oportunidades rival: $opponentGoalChances",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceColor,
                contentColor = teamColor,
                indicator = { tabPositions ->
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(2.dp)
                            .background(teamColor)
                    )
                },
                divider = {
                    Box(
                        modifier = Modifier
                            .height(1.dp)
                            .fillMaxWidth()
                            .background(BorderColor)
                    )
                }
            ) {
                listOf("Titulares", "Reservas").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedTab == index) teamColor else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(items = displayedStarters, key = { player -> player.id }) { player ->
                            val playerTime = vm.getFormattedPlayerTime(player.id, vm.currentMatch)
                            val (yellowCards, redCards) = playerCardCounts(player.id, match.events)

                            LivePlayerRowItem(
                                player = player,
                                role = "Titular",
                                teamColor = teamColor,
                                teamColorLight = teamColorLight,
                                playedTime = playerTime,
                                yellowCards = yellowCards,
                                redCards = redCards,
                                showStats = true,
                                showSwap = displayedSubstitutes.isNotEmpty(),
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

                        if (expelledPlayers.isNotEmpty()) {
                            item {
                                LiveBlockedSectionHeader(
                                    title = "Expulsados",
                                    count = expelledPlayers.size,
                                    accentColor = ErrorRed,
                                    accentLight = ErrorRedLight
                                )
                            }

                            items(items = expelledPlayers, key = { player -> player.id }) { player ->
                                val (yellowCards, redCards) = playerCardCounts(player.id, match.events)

                                LiveBlockedPlayerRowItem(
                                    player = player,
                                    reason = "Expulsado",
                                    accentColor = ErrorRed,
                                    yellowCards = yellowCards,
                                    redCards = redCards
                                )
                                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                            }
                        }

                        if (injuredPlayers.isNotEmpty()) {
                            item {
                                LiveBlockedSectionHeader(
                                    title = "Lesionados",
                                    count = injuredPlayers.size,
                                    accentColor = InjuryAmber,
                                    accentLight = InjuryLight
                                )
                            }

                            items(items = injuredPlayers, key = { player -> player.id }) { player ->
                                val (yellowCards, redCards) = playerCardCounts(player.id, match.events)

                                LiveBlockedPlayerRowItem(
                                    player = player,
                                    reason = "Lesionado",
                                    accentColor = InjuryAmber,
                                    yellowCards = yellowCards,
                                    redCards = redCards
                                )
                                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    if (displayedSubstitutes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sin reservas disponibles", color = TextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.padding(horizontal = 4.dp)) {
                            items(items = displayedSubstitutes, key = { player -> player.id }) { player ->
                                val playerTime = vm.getFormattedPlayerTime(player.id, vm.currentMatch)
                                val (yellowCards, redCards) = playerCardCounts(player.id, match.events)

                                LivePlayerRowItem(
                                    player = player,
                                    role = "Reserva",
                                    teamColor = teamColor,
                                    teamColorLight = teamColorLight,
                                    playedTime = playerTime,
                                    yellowCards = yellowCards,
                                    redCards = redCards
                                )
                                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showOpponentEventDialog) {
        AlertDialog(
            onDismissRequest = { showOpponentEventDialog = false },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Registrar rival",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "¿Qué deseas registrar para el rival?",
                        color = TextSecondary
                    )

                    TextButton(
                        onClick = {
                            vm.updateOpponentGoals(1)
                            showOpponentEventDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Text("Gol", fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = {
                            vm.updateOpponentGoalChances(1)
                            showOpponentEventDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = teamColor)
                    ) {
                        Text("Oportunidad de Gol", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOpponentEventDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    if (showStopMatchDialog) {
        AlertDialog(
            onDismissRequest = { showStopMatchDialog = false },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Finalizar partido", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("¿Estás seguro de finalizar el partido?", color = TextSecondary)
                    Text(
                        "Rival: $opponentGoals goles · $opponentGoalChances oportunidades",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopMatchDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.stopMatch()
                        showStopMatchDialog = false
                    }
                ) {
                    Text("Finalizar", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (showSwapDialog && starterToSwap != null) {
        MatchSwapDialog(
            starter = starterToSwap!!,
            substitutes = displayedSubstitutes,
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
                    vm.registerSwap(starter = starter, sub = substitute)
                    Toast.makeText(
                        navHostController.context,
                        "Cambio registrado",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        navHostController.context,
                        "Selecciona una reserva",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@MatchSwapDialog
                }
                showSwapDialog = false
                starterToSwap = null
                selectedSubstitute = null
            }
        )
    }
}

@Composable
private fun MatchScoreHeader(
    teamName: String,
    rivalName: String,
    ownGoals: Int,
    opponentGoals: Int,
    onOpponentPlus: () -> Unit,
    onOpponentMinus: () -> Unit,
    teamColor: Color,
    teamColorLight: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$teamName VS $rivalName",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreViewBox(
                    title = teamName,
                    value = ownGoals,
                    accentColor = teamColor,
                    accentLight = teamColorLight,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "VS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.width(14.dp))

                RivalScoreControlBox(
                    title = rivalName,
                    value = opponentGoals,
                    onPlus = onOpponentPlus,
                    onMinus = onOpponentMinus,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScoreViewBox(
    title: String,
    value: Int,
    accentColor: Color,
    accentLight: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accentLight)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = accentColor
            )
        }
    }
}

@Composable
private fun RivalScoreControlBox(
    title: String,
    value: Int,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF5F5F5))
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallSquareActionButton(
                    symbol = Icons.Default.Add,
                    onClick = onPlus
                )
                SmallSquareActionButton(
                    symbol = Icons.Default.Remove,
                    onClick = onMinus
                )
            }
        }
    }
}

@Composable
private fun SmallSquareActionButton(
    symbol: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceColor)
            .border(1.dp, BorderColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = symbol,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun LiveBlockedSectionHeader(
    title: String,
    count: Int,
    accentColor: Color,
    accentLight: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accentLight)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = accentColor,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.75f))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun LiveBlockedPlayerRowItem(
    player: Player,
    reason: String,
    accentColor: Color,
    yellowCards: Int = 0,
    redCards: Int = 0
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BlockedGray)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.number.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BlockedText
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = BlockedText
            )
            Text(
                text = "Bloqueado · $reason · N° ${player.number}",
                fontSize = 12.sp,
                color = accentColor
            )

            if (yellowCards > 0 || redCards > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                MiniCardIndicatorRow(
                    yellowCount = yellowCards,
                    redCount = redCards
                )
            }
        }

        Icon(
            imageVector = if (reason == "Expulsado") Icons.Default.Warning else Icons.Default.Healing,
            contentDescription = reason,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun LivePlayerRowItem(
    player: Player,
    role: String,
    teamColor: Color,
    teamColorLight: Color,
    playedTime: String = "00:00",
    yellowCards: Int = 0,
    redCards: Int = 0,
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
                .background(if (role == "Titular") teamColorLight else Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.number.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (role == "Titular") teamColor else TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = TextPrimary
            )
            Text(
                text = "$role · N° ${player.number} · $playedTime",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showStats) {
                    IconButton(onClick = onStats, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.StackedLineChart,
                            contentDescription = "Estadísticas",
                            tint = teamColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (showSwap) {
                    IconButton(onClick = onSwap, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Cambio",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (yellowCards > 0 || redCards > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                MiniCardIndicatorRow(
                    yellowCount = yellowCards,
                    redCount = redCards
                )
            }
        }
    }
}

@Composable
private fun MiniCardIndicatorRow(
    yellowCount: Int,
    redCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(yellowCount.coerceAtMost(2)) {
            MiniFootballCard(color = YellowCardColor)
        }

        repeat(redCount.coerceAtMost(1)) {
            MiniFootballCard(color = ErrorRed)
        }
    }
}

@Composable
private fun MiniFootballCard(color: Color) {
    Box(
        modifier = Modifier
            .size(width = 10.dp, height = 14.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
    )
}