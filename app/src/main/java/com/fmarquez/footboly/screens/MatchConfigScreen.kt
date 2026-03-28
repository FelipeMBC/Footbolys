package com.fmarquez.footboly.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.util.hexToColor
import com.fmarquez.footboly.util.teamColorLight
import com.fmarquez.footboly.vm.FutbolViewModel
import java.util.Calendar

private val BgColor       = Color(0xFFF7F7F5)
private val SurfaceColor  = Color(0xFFFFFFFF)
private val TextPrimary   = Color(0xFF111111)
private val TextSecondary = Color(0xFF888888)
private val BorderColor   = Color(0xFFE0E0DC)
private val BlockedGray   = Color(0xFFEAEAEA)
private val BlockedText   = Color(0xFF8C8C8C)
private val ErrorRed      = Color(0xFFD32F2F)
private val ErrorRedLight = Color(0xFFFFF1F1)
private val InjuryAmber   = Color(0xFFE65100)
private val InjuryLight   = Color(0xFFFFF3E0)

private val DIAS  = listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
private val MESES = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")

private fun formatDateLabel(year: Int, month: Int, day: Int, hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply { set(year, month, day) }
    val dia = DIAS[cal.get(Calendar.DAY_OF_WEEK) - 1]
    val mes = MESES[month]
    val h = hour.toString().padStart(2, '0')
    val m = minute.toString().padStart(2, '0')
    return "$dia $day $mes $year · $h:$m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchConfigScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val team = vm.selectedTeam ?: return
    val match = vm.currentMatch ?: return
    val context = LocalContext.current

    val teamColor = hexToColor(team.shirtColorHex)
    val teamColorLight = teamColorLight(teamColor)

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showSetupDialog by remember { mutableStateOf(false) }
    var showLessThanElevenDialog by remember { mutableStateOf(false) }

    var rivalName by remember { mutableStateOf("") }
    var matchDurationMinutes by remember { mutableFloatStateOf(60f) }

    val nowCal = remember { Calendar.getInstance() }
    var selYear by remember { mutableIntStateOf(nowCal.get(Calendar.YEAR)) }
    var selMonth by remember { mutableIntStateOf(nowCal.get(Calendar.MONTH)) }
    var selDay by remember { mutableIntStateOf(nowCal.get(Calendar.DAY_OF_MONTH)) }
    var selHour by remember { mutableIntStateOf(nowCal.get(Calendar.HOUR_OF_DAY)) }
    var selMinute by remember { mutableIntStateOf(nowCal.get(Calendar.MINUTE)) }
    var dateChosen by remember { mutableStateOf(false) }
    var timeChosen by remember { mutableStateOf(false) }

    LaunchedEffect(match.isStarted, match.isFinished) {
        if (match.isStarted && !match.isFinished) {
            navHostController.navigate(Screen.MATCH_LIVE.route) {
                popUpTo(Screen.MATCH_CONFIG.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    fun openSetupDialog() {
        rivalName = match.rivalName
        matchDurationMinutes = ((match.totalSeconds / 60).coerceIn(10, 90)).toFloat()
        showSetupDialog = true
    }

    fun confirmStart() {
        val dateLabel = if (dateChosen || timeChosen) {
            formatDateLabel(selYear, selMonth, selDay, selHour, selMinute)
        } else {
            ""
        }

        vm.setMatchRivalAndDate(rivalName.trim(), dateLabel)
        vm.setMatchDuration(matchDurationMinutes.toInt())
        vm.startMatch()

        navHostController.navigate(Screen.MATCH_LIVE.route) {
            popUpTo(Screen.MATCH_CONFIG.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    val availableForStart = match.starters.size + match.substitutes.size

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            match.isFinished -> "Partido terminado"
                            match.isStarted -> vm.getFormattedMatchTime()
                            else -> team.name
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
                                Toast.makeText(
                                    context,
                                    "No puedes volver mientras el partido está en curso",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                navHostController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (!match.isStarted && !match.isFinished) {
                        IconButton(
                            onClick = {
                                if (availableForStart < 5) {
                                    Toast.makeText(
                                        context,
                                        "Selecciona al menos 5 jugadores disponibles para iniciar",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@IconButton
                                }

                                if (match.starters.size < 11) {
                                    showLessThanElevenDialog = true
                                } else {
                                    openSetupDialog()
                                }
                            },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(teamColor)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Iniciar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
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
                val tabs = listOf(
                    "Titulares (${match.starters.size})",
                    "Reservas (${match.substitutes.size})",
                    "Expulsados (${match.expelledPlayers.size})",
                    "Lesionados (${match.injuredPlayers.size})"
                )

                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedTab == index) teamColor else TextSecondary
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
                val infoText = when (selectedTab) {
                    0 -> "Selecciona hasta 11 titulares"
                    1 -> "Selecciona las reservas disponibles"
                    2 -> "Jugadores apartados por expulsión previa"
                    else -> "Jugadores apartados por lesión"
                }

                Text(
                    text = infoText,
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Disponibles para iniciar: $availableForStart · Titulares: ${match.starters.size}/11",
                    fontSize = 12.sp,
                    color = if (availableForStart >= 5) teamColor else TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        SelectablePlayersList(
                            allPlayers = team.players,
                            selectedPlayers = match.starters,
                            blockedPlayers = match.substitutes + match.expelledPlayers + match.injuredPlayers,
                            enabled = !match.isStarted && !match.isFinished,
                            accentColor = teamColor,
                            accentColorLight = teamColorLight,
                            blockedBackground = BlockedGray,
                            blockedTextColor = BlockedText,
                            max = 11,
                            blockedReason = { player ->
                                when {
                                    match.substitutes.any { it.id == player.id } -> "En reservas"
                                    match.expelledPlayers.any { it.id == player.id } -> "Expulsado"
                                    match.injuredPlayers.any { it.id == player.id } -> "Lesionado"
                                    else -> ""
                                }
                            },
                            onToggle = { vm.toggleStarter(it) }
                        )
                    }

                    1 -> {
                        SelectablePlayersList(
                            allPlayers = team.players,
                            selectedPlayers = match.substitutes,
                            blockedPlayers = match.starters + match.expelledPlayers + match.injuredPlayers,
                            enabled = !match.isStarted && !match.isFinished,
                            accentColor = teamColor,
                            accentColorLight = teamColorLight,
                            blockedBackground = BlockedGray,
                            blockedTextColor = BlockedText,
                            max = team.players.size,
                            blockedReason = { player ->
                                when {
                                    match.starters.any { it.id == player.id } -> "Titular"
                                    match.expelledPlayers.any { it.id == player.id } -> "Expulsado"
                                    match.injuredPlayers.any { it.id == player.id } -> "Lesionado"
                                    else -> ""
                                }
                            },
                            onToggle = { vm.toggleSubstitute(it) }
                        )
                    }

                    2 -> {
                        SelectablePlayersList(
                            allPlayers = team.players,
                            selectedPlayers = match.expelledPlayers,
                            blockedPlayers = match.starters + match.substitutes + match.injuredPlayers,
                            enabled = !match.isStarted && !match.isFinished,
                            accentColor = ErrorRed,
                            accentColorLight = ErrorRedLight,
                            blockedBackground = BlockedGray,
                            blockedTextColor = BlockedText,
                            max = team.players.size,
                            blockedReason = { player ->
                                when {
                                    match.starters.any { it.id == player.id } -> "Titular"
                                    match.substitutes.any { it.id == player.id } -> "Reserva"
                                    match.injuredPlayers.any { it.id == player.id } -> "Lesionado"
                                    else -> ""
                                }
                            },
                            onToggle = { vm.toggleExpelled(it) }
                        )
                    }

                    3 -> {
                        SelectablePlayersList(
                            allPlayers = team.players,
                            selectedPlayers = match.injuredPlayers,
                            blockedPlayers = match.starters + match.substitutes + match.expelledPlayers,
                            enabled = !match.isStarted && !match.isFinished,
                            accentColor = InjuryAmber,
                            accentColorLight = InjuryLight,
                            blockedBackground = BlockedGray,
                            blockedTextColor = BlockedText,
                            max = team.players.size,
                            blockedReason = { player ->
                                when {
                                    match.starters.any { it.id == player.id } -> "Titular"
                                    match.substitutes.any { it.id == player.id } -> "Reserva"
                                    match.expelledPlayers.any { it.id == player.id } -> "Expulsado"
                                    else -> ""
                                }
                            },
                            onToggle = { vm.toggleInjured(it) }
                        )
                    }
                }
            }
        }
    }

    if (showLessThanElevenDialog) {
        AlertDialog(
            onDismissRequest = { showLessThanElevenDialog = false },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Menos de 11 titulares",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    "Has seleccionado menos de 11 titulares. ¿Deseas continuar de todas formas?",
                    color = TextSecondary
                )
            },
            dismissButton = {
                TextButton(onClick = { showLessThanElevenDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLessThanElevenDialog = false
                        openSetupDialog()
                    }
                ) {
                    Text("Continuar", color = teamColor, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (showSetupDialog) {
        Dialog(
            onDismissRequest = { showSetupDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Configurar partido",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = rivalName,
                        onValueChange = { rivalName = it },
                        label = { Text("Rival (opcional)", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = teamColor,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = teamColor,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = teamColor,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = teamColor
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (dateChosen) teamColor.copy(alpha = 0.5f) else BorderColor,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, mo, d ->
                                        selYear = y
                                        selMonth = mo
                                        selDay = d
                                        dateChosen = true
                                    },
                                    selYear,
                                    selMonth,
                                    selDay
                                ).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = if (dateChosen) teamColor else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fecha del partido", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = if (dateChosen) {
                                    val cal = Calendar.getInstance().apply { set(selYear, selMonth, selDay) }
                                    "${DIAS[cal.get(Calendar.DAY_OF_WEEK) - 1]} $selDay ${MESES[selMonth]} $selYear"
                                } else {
                                    "Toca para seleccionar"
                                },
                                fontSize = 14.sp,
                                fontWeight = if (dateChosen) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (dateChosen) TextPrimary else TextSecondary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                if (timeChosen) teamColor.copy(alpha = 0.5f) else BorderColor,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        selHour = h
                                        selMinute = m
                                        timeChosen = true
                                    },
                                    selHour,
                                    selMinute,
                                    true
                                ).show()
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (timeChosen) teamColor else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hora de inicio", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                text = if (timeChosen) {
                                    "${selHour.toString().padStart(2, '0')}:${selMinute.toString().padStart(2, '0')}"
                                } else {
                                    "Toca para seleccionar"
                                },
                                fontSize = 14.sp,
                                fontWeight = if (timeChosen) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (timeChosen) TextPrimary else TextSecondary
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Duración del partido",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(teamColorLight)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${matchDurationMinutes.toInt()} min",
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = teamColor
                            )
                        }
                        Text("Entre 10 y 90 minutos", fontSize = 11.sp, color = TextSecondary)
                        Slider(
                            value = matchDurationMinutes,
                            onValueChange = { matchDurationMinutes = it },
                            valueRange = 10f..90f,
                            steps = 79,
                            colors = SliderDefaults.colors(
                                thumbColor = teamColor,
                                activeTrackColor = teamColor,
                                inactiveTrackColor = BorderColor
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showSetupDialog = false }) {
                            Text("Cancelar", color = TextSecondary)
                        }

                        TextButton(
                            onClick = {
                                showSetupDialog = false
                                confirmStart()
                            }
                        ) {
                            Text("Iniciar", color = teamColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectablePlayersList(
    allPlayers: List<Player>,
    selectedPlayers: List<Player>,
    blockedPlayers: List<Player> = emptyList(),
    enabled: Boolean = true,
    accentColor: Color = Color(0xFF1E6B45),
    accentColorLight: Color = Color(0xFFE8F2EC),
    blockedBackground: Color = Color(0xFFEAEAEA),
    blockedTextColor: Color = Color(0xFF8C8C8C),
    max: Int,
    blockedReason: (Player) -> String = { "" },
    onToggle: (Player) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(allPlayers, key = { it.id }) { player ->
            val isSelected = selectedPlayers.any { it.id == player.id }
            val isBlocked = blockedPlayers.any { it.id == player.id }

            val cardBg = when {
                !enabled -> Color(0xFFF0F0EE)
                isBlocked -> blockedBackground
                isSelected -> accentColorLight
                else -> SurfaceColor
            }

            val border = when {
                isSelected -> accentColor.copy(alpha = 0.35f)
                else -> BorderColor
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, border, RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled && (!isBlocked) && (isSelected || selectedPlayers.size < max)) {
                        onToggle(player)
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> accentColor
                                    isBlocked -> Color.White
                                    else -> BorderColor
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = player.number.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected -> Color.White
                                isBlocked -> blockedTextColor
                                else -> TextSecondary
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = player.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = when {
                                isBlocked -> blockedTextColor
                                else -> TextPrimary
                            }
                        )

                        val subtitle = when {
                            !enabled -> "Bloqueado"
                            isBlocked -> blockedReason(player)
                            isSelected -> "Seleccionado"
                            else -> "Disponible"
                        }

                        if (subtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 12.sp,
                                color = when {
                                    isSelected -> accentColor
                                    isBlocked -> blockedTextColor
                                    else -> TextSecondary
                                }
                            )
                        }
                    }

                    Text(
                        text = when {
                            !enabled -> "Bloqueado"
                            isBlocked -> "—"
                            isSelected -> "✓"
                            else -> ""
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isSelected -> accentColor
                            isBlocked -> blockedTextColor
                            else -> TextSecondary
                        }
                    )
                }
            }
        }
    }
}