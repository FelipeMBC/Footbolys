package com.fmarquez.footboly.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.modelos.PlayerStatsDraft
import com.fmarquez.footboly.util.hexToColor
import com.fmarquez.footboly.util.teamColorLight
import com.fmarquez.footboly.vm.FutbolViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow


private val BgColor = Color(0xFFF7F7F5)
private val SurfaceColor = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF111111)
private val TextSecondary = Color(0xFF888888)
private val BorderColor = Color(0xFFE0E0DC)
private val ErrorRed = Color(0xFFD32F2F)
private val ErrorRedLight = Color(0xFFFFF1F1)
private val YellowCardColor = Color(0xFFF2B705)
private val YellowCardLight = Color(0xFFFFF8DD)

private object StatKey {
    const val GOL_FAVOR = "GOL_FAVOR"
    const val GOL_CONTRA = "GOL_CONTRA"
    const val TIRO_ARCO_POS = "TIRO_ARCO_POS"
    const val TIRO_ARCO_NEG = "TIRO_ARCO_NEG"
    const val PART_GOL_FAVOR = "PART_GOL_FAVOR"
    const val PART_GOL_CONTRA = "PART_GOL_CONTRA"
    const val REMATE12_POS = "REMATE12_POS"
    const val REMATE12_NEG = "REMATE12_NEG"

    const val BALON_RECUPERADO = "BALON_RECUPERADO"

    const val BALON_PERDIDO = "BALON_PERDIDO"
    const val PASES_BUENOS = "PASES_BUENOS"
    const val PASES_MALOS = "PASES_MALOS"
    const val CENTROS_POS = "CENTROS_POS"
    const val CENTROS_NEG = "CENTROS_NEG"
    const val RECHAZOS_POS = "RECHAZOS_POS"
    const val RECHAZOS_NEG = "RECHAZOS_NEG"

    const val FALTA_FAVOR = "FALTA_FAVOR"
    const val FALTA_CONTRA = "FALTA_CONTRA"
    const val CORNER_POS = "CORNER_POS"
    const val CORNER_NEG = "CORNER_NEG"
    const val OFFSIDE_FAVOR = "OFFSIDE_FAVOR"
    const val OFFSIDE_CONTRA = "OFFSIDE_CONTRA"
    const val PENAL_FAVOR = "PENAL_FAVOR"
    const val PENAL_CONTRA = "PENAL_CONTRA"

    const val AMARILLA = "AMARILLA"
    const val ROJA = "ROJA"
}

private enum class StatsSection {
    GOL,
    JUEGO,
    FALTAS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerStatsScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val context = LocalContext.current
    val player = vm.getSelectedPlayer() ?: return
    val team = vm.selectedTeam ?: return
    val activeMatch = vm.getActiveMatchForStats() ?: return
    val isEditingFinishedMatch = vm.isEditingFinishedMatchMode()

    val teamColor = hexToColor(team.shirtColorHex)
    val teamColorLight = teamColorLight(teamColor)

    val role = when {
        activeMatch.starters.any { it.id == player.id } -> "Titular"
        activeMatch.substitutes.any { it.id == player.id } -> "Reserva"
        else -> ""
    }

    var selectedSection by rememberSaveable { mutableStateOf(StatsSection.GOL) }

    val currentDraft = vm.getOrCreatePlayerStatsDraft(player.id)

    val isMatchExpelled = activeMatch.expelledPlayers.any { it.id == player.id }
    val isDraftExpelled = currentDraft.amarilla >= 2 || currentDraft.roja > 0
    val isPlayerExpelled = isMatchExpelled || isDraftExpelled

    val expulsionReason = when {
        currentDraft.roja > 0 -> "Tarjeta Roja"
        currentDraft.amarilla >= 2 -> "Doble Amarilla"
        isMatchExpelled -> "Expulsado"
        else -> ""
    }

    var showCardDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingSelection by remember { mutableStateOf<String?>(null) }

    val yellowCount = currentDraft.amarilla.coerceIn(0, 2)
    val redCount = currentDraft.roja.coerceIn(0, 1)

    fun applyCardSelection(selection: String) {
        val latestDraft = vm.getOrCreatePlayerStatsDraft(player.id)

        when (selection) {
            "AMARILLA" -> {
                val updated = latestDraft.copy(
                    amarilla = (latestDraft.amarilla + 1).coerceAtMost(2)
                )
                vm.updatePlayerStatsDraft(updated)

                if (updated.amarilla >= 2 && !isEditingFinishedMatch) {
                    vm.savePlayerStatsDraftAsEvents(player.id)
                    vm.expelPlayerByCard(player, "Doble Amarilla")
                    navHostController.popBackStack()
                }
            }

            "DOBLE_AMARILLA" -> {
                val updated = latestDraft.copy(
                    amarilla = 2,
                    roja = 0
                )
                vm.updatePlayerStatsDraft(updated)

                if (!isEditingFinishedMatch) {
                    vm.savePlayerStatsDraftAsEvents(player.id)
                    vm.expelPlayerByCard(player, "Doble Amarilla")
                    navHostController.popBackStack()
                }
            }

            "ROJA" -> {
                val updated = latestDraft.copy(
                    roja = 1
                )
                vm.updatePlayerStatsDraft(updated)

                if (!isEditingFinishedMatch) {
                    vm.savePlayerStatsDraftAsEvents(player.id)
                    vm.expelPlayerByCard(player, "Tarjeta Roja")
                    navHostController.popBackStack()
                }
            }
        }
    }

    fun needsConfirmation(selection: String): Boolean {
        val latestDraft = vm.getOrCreatePlayerStatsDraft(player.id)
        return when (selection) {
            "ROJA" -> true
            "DOBLE_AMARILLA" -> true
            "AMARILLA" -> latestDraft.amarilla + 1 >= 2
            else -> false
        }
    }

    fun confirmationText(selection: String): String {
        return when (selection) {
            "ROJA" -> "¿Estás seguro de asignarle Tarjeta Roja? Al hacer esto el jugador quedará bloqueado."
            "DOBLE_AMARILLA" -> "¿Estás seguro de asignarle Doble Amarilla? Al hacer esto el jugador quedará bloqueado."
            "AMARILLA" -> "¿Estás seguro de asignarle esta Amarilla? Con esta cantidad el jugador quedará bloqueado."
            else -> ""
        }
    }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditingFinishedMatch) "Editar estadísticas" else "Estadísticas",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val savedLines = vm.savePlayerStatsDraftAsEvents(player.id)
                            if (isEditingFinishedMatch) {
                                Toast.makeText(
                                    context,
                                    if (savedLines.isEmpty()) "No hubo cambios" else "Cambios guardados",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navHostController.popBackStack()
                            } else {
                                val toastText = if (savedLines.isEmpty()) {
                                    "Sin cambios para registrar"
                                } else {
                                    (savedLines + "Registrado").joinToString("\n")
                                }
                                Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(teamColor)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Guardar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Registro estadístico",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlayerHeaderStatsCard(
                player = player,
                role = role,
                teamEmoji = team.logoEmoji,
                teamColor = teamColor,
                teamColorLight = teamColorLight,
                isEditingFinishedMatch = isEditingFinishedMatch,
                yellowCount = yellowCount,
                redCount = redCount,
                canAddCard = !isPlayerExpelled,
                onCardsClick = { showCardDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            QuickStatsBlocksRow(
                teamColor = teamColor,
                teamColorLight = teamColorLight,
                selectedSection = selectedSection,
                onGolClick = { selectedSection = StatsSection.GOL },
                onJuegoClick = { selectedSection = StatsSection.JUEGO },
                onDetenidoClick = { selectedSection = StatsSection.FALTAS }
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isPlayerExpelled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ErrorRedLight)
                        .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Jugador expulsado",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = ErrorRed
                        )
                        if (expulsionReason.isNotEmpty()) {
                            Text(
                                text = "Motivo: $expulsionReason",
                                fontSize = 12.sp,
                                color = ErrorRed.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            StatsSectionContent(
                selectedSection = selectedSection,
                currentDraft = currentDraft,
                player = player,
                vm = vm,
                teamColor = teamColor,
                teamColorLight = teamColorLight
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showCardDialog) {
        CardSelectionDialog(
            currentYellowCards = yellowCount,
            onDismiss = { showCardDialog = false },
            onSelectYellow = {
                showCardDialog = false
                if (needsConfirmation("AMARILLA")) {
                    pendingSelection = "AMARILLA"
                    showConfirmDialog = true
                } else {
                    applyCardSelection("AMARILLA")
                }
            },
            onSelectDoubleYellow = {
                showCardDialog = false
                pendingSelection = "DOBLE_AMARILLA"
                showConfirmDialog = true
            },
            onSelectRed = {
                showCardDialog = false
                pendingSelection = "ROJA"
                showConfirmDialog = true
            }
        )
    }

    if (showConfirmDialog && pendingSelection != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                pendingSelection = null
            },
            title = {
                Text("Confirmar tarjeta", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(confirmationText(pendingSelection!!))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selection = pendingSelection
                        showConfirmDialog = false
                        pendingSelection = null
                        if (selection != null) {
                            applyCardSelection(selection)
                        }
                    }
                ) {
                    Text("Confirmar", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        pendingSelection = null
                    }
                ) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PlayerHeaderStatsCard(
    player: Player,
    role: String,
    teamEmoji: String,
    teamColor: Color,
    teamColorLight: Color,
    isEditingFinishedMatch: Boolean,
    yellowCount: Int,
    redCount: Int,
    canAddCard: Boolean,
    onCardsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(teamColorLight),
                contentAlignment = Alignment.Center
            ) {
                Text(teamEmoji, fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = player.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )

                Text(
                    text = if (role.isNotEmpty()) "$role · N° ${player.number}" else "N° ${player.number}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isEditingFinishedMatch) Color(0xFFFFF3E0) else teamColorLight)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isEditingFinishedMatch) "Edición" else "En curso",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isEditingFinishedMatch) Color(0xFFE65100) else teamColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniCardsIndicator(
                    yellowCount = yellowCount,
                    redCount = redCount
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canAddCard) teamColor else Color(0xFFE7E7E7))
                        .clickable(enabled = canAddCard, onClick = onCardsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Tarjetas",
                        tint = if (canAddCard) Color.White else Color(0xFF9A9A9A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniCardsIndicator(
    yellowCount: Int,
    redCount: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(yellowCount) {
                FootballCardVisual(
                    color = YellowCardColor,
                    width = 14,
                    height = 20
                )
            }

            repeat(redCount) {
                FootballCardVisual(
                    color = ErrorRed,
                    width = 14,
                    height = 20
                )
            }

            if (yellowCount == 0 && redCount == 0) {
                repeat(2) {
                    FootballCardVisual(
                        color = BorderColor.copy(alpha = 0.35f),
                        width = 14,
                        height = 20
                    )
                }
            }
        }

        Text(
            text = when {
                redCount > 0 -> "Roja"
                yellowCount == 2 -> "2 Amarillas"
                yellowCount == 1 -> "1 Amarilla"
                else -> "Sin tarjetas"
            },
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun QuickStatsBlocksRow(
    teamColor: Color,
    teamColorLight: Color,
    selectedSection: StatsSection,
    onGolClick: () -> Unit,
    onJuegoClick: () -> Unit,
    onDetenidoClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickStatBlock(
            modifier = Modifier.weight(1f),
            title = "Gol",
            icon = Icons.Default.SportsSoccer,
            isSelected = selectedSection == StatsSection.GOL,
            accentColor = teamColor,
            accentLight = teamColorLight,
            onClick = onGolClick
        )

        QuickStatBlock(
            modifier = Modifier.weight(1f),
            title = "Juego",
            icon = Icons.Default.Star,
            isSelected = selectedSection == StatsSection.JUEGO,
            accentColor = teamColor,
            accentLight = teamColorLight,
            onClick = onJuegoClick
        )

        QuickStatBlock(
            modifier = Modifier.weight(1f),
            title = "Faltas",
            icon = Icons.Default.Warning,
            isSelected = selectedSection == StatsSection.FALTAS,
            accentColor = teamColor,
            accentLight = teamColorLight,
            onClick = onDetenidoClick
        )
    }
}

@Composable
private fun QuickStatBlock(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    accentLight: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .border(
                1.dp,
                if (isSelected) accentColor.copy(alpha = 0.35f) else BorderColor,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentLight else SurfaceColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor else BorderColor.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isSelected) Color.White else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) accentColor else TextPrimary
            )
        }
    }
}

@Composable
private fun FootballCardVisual(
    color: Color,
    width: Int = 20,
    height: Int = 28
) {
    Box(
        modifier = Modifier
            .size(width = width.dp, height = height.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
    )
}

@Composable
private fun CardOptionItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    backgroundColor: Color,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor)
            Text(subtitle, fontSize = 12.sp, color = textColor.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun CardSelectionDialog(
    currentYellowCards: Int,
    onDismiss: () -> Unit,
    onSelectYellow: () -> Unit,
    onSelectDoubleYellow: () -> Unit,
    onSelectRed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Registrar tarjeta",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Selecciona el tipo de tarjeta",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                if (currentYellowCards == 0) {
                    CardOptionItem(
                        icon = { FootballCardVisual(YellowCardColor) },
                        title = "Amarilla",
                        subtitle = "Primera advertencia",
                        backgroundColor = YellowCardLight,
                        borderColor = YellowCardColor.copy(alpha = 0.4f),
                        textColor = Color(0xFF8A6800),
                        onClick = onSelectYellow
                    )
                }

                if (currentYellowCards <= 1) {
                    CardOptionItem(
                        icon = {
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                FootballCardVisual(YellowCardColor)
                                FootballCardVisual(YellowCardColor)
                            }
                        },
                        title = if (currentYellowCards == 1) "Segunda Amarilla" else "Doble Amarilla",
                        subtitle = "Expulsión del jugador",
                        backgroundColor = ErrorRedLight,
                        borderColor = ErrorRed.copy(alpha = 0.35f),
                        textColor = ErrorRed,
                        onClick = onSelectDoubleYellow
                    )
                }

                HorizontalDivider(color = BorderColor)

                CardOptionItem(
                    icon = { FootballCardVisual(ErrorRed) },
                    title = "Tarjeta Roja",
                    subtitle = "Expulsión directa",
                    backgroundColor = ErrorRedLight,
                    borderColor = ErrorRed.copy(alpha = 0.35f),
                    textColor = ErrorRed,
                    onClick = onSelectRed
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun StatsSectionContent(
    selectedSection: StatsSection,
    currentDraft: PlayerStatsDraft,
    player: Player,
    vm: FutbolViewModel,
    teamColor: Color,
    teamColorLight: Color
) {
    when (selectedSection) {
        StatsSection.GOL -> StatsContentCard(
            title = "Gol",
            icon = Icons.Default.SportsSoccer,
            accentColor = teamColor,
            accentLight = teamColorLight
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactFavorContraCard(
                        modifier = Modifier.weight(1f),
                        title = "Gol",
                        favorValue = draftValue(currentDraft, StatKey.GOL_FAVOR),
                        contraValue = draftValue(currentDraft, StatKey.GOL_CONTRA),
                        favorAccent = teamColor,
                        favorLight = teamColorLight,
                        contraAccent = ErrorRed,
                        contraLight = ErrorRedLight,
                        leftLabel = "Contra",
                        rightLabel = "Favor",
                        onFavorIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.GOL_FAVOR
                                )
                            )
                        },
                        onFavorDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.GOL_FAVOR
                                )
                            )
                        },
                        onContraIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.GOL_CONTRA
                                )
                            )
                        },
                        onContraDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.GOL_CONTRA
                                )
                            )
                        }
                    )

                    CompactFavorContraCard(
                        modifier = Modifier.weight(1f),
                        title = "Asistencia",
                        favorValue = draftValue(currentDraft, StatKey.PART_GOL_FAVOR),
                        contraValue = draftValue(currentDraft, StatKey.PART_GOL_CONTRA),
                        favorAccent = teamColor,
                        favorLight = teamColorLight,
                        contraAccent = ErrorRed,
                        contraLight = ErrorRedLight,
                        leftLabel = "Contra",
                        rightLabel = "Favor",
                        onFavorIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.PART_GOL_FAVOR
                                )
                            )
                        },
                        onFavorDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.PART_GOL_FAVOR
                                )
                            )
                        },
                        onContraIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.PART_GOL_CONTRA
                                )
                            )
                        },
                        onContraDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.PART_GOL_CONTRA
                                )
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactSingleCounterCard(
                        modifier = Modifier.weight(1f),
                        title = "Tiro Arco",
                        value = draftValue(currentDraft, StatKey.TIRO_ARCO_POS),
                        accentColor = teamColor,
                        lightColor = teamColorLight,
                        onIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.TIRO_ARCO_POS
                                )
                            )
                        },
                        onDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.TIRO_ARCO_POS
                                )
                            )
                        }
                    )

                    CompactSingleCounterCard(
                        modifier = Modifier.weight(1f),
                        title = "Remate 1/2",
                        value = draftValue(currentDraft, StatKey.REMATE12_POS),
                        accentColor = teamColor,
                        lightColor = teamColorLight,
                        onIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.REMATE12_POS
                                )
                            )
                        },
                        onDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.REMATE12_POS
                                )
                            )
                        }
                    )
                }
            }
        }

        StatsSection.JUEGO -> StatsContentCard(
            title = "Recuperación / Juego",
            icon = Icons.Default.Star,
            accentColor = teamColor,
            accentLight = teamColorLight
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactFavorContraCard(
                        modifier = Modifier.weight(1f),
                        title = "Balón",
                        favorValue = draftValue(currentDraft, StatKey.BALON_RECUPERADO),
                        contraValue = draftValue(currentDraft, StatKey.BALON_PERDIDO),
                        favorAccent = teamColor,
                        favorLight = teamColorLight,
                        contraAccent = ErrorRed,
                        contraLight = ErrorRedLight,
                        leftLabel = "Perdido",
                        rightLabel = "Recup.",
                        onFavorIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.BALON_RECUPERADO
                                )
                            )
                        },
                        onFavorDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.BALON_RECUPERADO
                                )
                            )
                        },
                        onContraIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.BALON_PERDIDO
                                )
                            )
                        },
                        onContraDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.BALON_PERDIDO
                                )
                            )
                        }
                    )

                    CompactFavorContraCard(
                        modifier = Modifier.weight(1f),
                        title = "Pases",
                        favorValue = draftValue(currentDraft, StatKey.PASES_BUENOS),
                        contraValue = draftValue(currentDraft, StatKey.PASES_MALOS),
                        favorAccent = teamColor,
                        favorLight = teamColorLight,
                        contraAccent = ErrorRed,
                        contraLight = ErrorRedLight,
                        leftLabel = "Malo",
                        rightLabel = "Bueno",
                        onFavorIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.PASES_BUENOS
                                )
                            )
                        },
                        onFavorDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.PASES_BUENOS
                                )
                            )
                        },
                        onContraIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.PASES_MALOS
                                )
                            )
                        },
                        onContraDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.PASES_MALOS
                                )
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactSingleCounterCard(
                        modifier = Modifier.weight(1f),
                        title = "Centro",
                        value = draftValue(currentDraft, StatKey.CENTROS_POS),
                        accentColor = teamColor,
                        lightColor = teamColorLight,
                        onIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.CENTROS_POS
                                )
                            )
                        },
                        onDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.CENTROS_POS
                                )
                            )
                        }
                    )

                    CompactSingleCounterCard(
                        modifier = Modifier.weight(1f),
                        title = "Rechazo",
                        value = draftValue(currentDraft, StatKey.RECHAZOS_POS),
                        accentColor = teamColor,
                        lightColor = teamColorLight,
                        onIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.RECHAZOS_POS
                                )
                            )
                        },
                        onDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.RECHAZOS_POS
                                )
                            )
                        }
                    )
                }
            }
        }

        StatsSection.FALTAS -> StatsContentCard(
            title = "Faltas / Balón detenido",
            icon = Icons.Default.Warning,
            accentColor = teamColor,
            accentLight = teamColorLight
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactFavorContraCard(
                        modifier = Modifier.weight(1f),
                        title = "Penal",
                        favorValue = draftValue(currentDraft, StatKey.PENAL_FAVOR),
                        contraValue = draftValue(currentDraft, StatKey.PENAL_CONTRA),
                        favorAccent = teamColor,
                        favorLight = teamColorLight,
                        contraAccent = ErrorRed,
                        contraLight = ErrorRedLight,
                        leftLabel = "Contra",
                        rightLabel = "Favor",
                        onFavorIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.PENAL_FAVOR
                                )
                            )
                        },
                        onFavorDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.PENAL_FAVOR
                                )
                            )
                        },
                        onContraIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.PENAL_CONTRA
                                )
                            )
                        },
                        onContraDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.PENAL_CONTRA
                                )
                            )
                        }
                    )

                    CompactFavorContraCard(
                        modifier = Modifier.weight(1f),
                        title = "Off Side",
                        favorValue = draftValue(currentDraft, StatKey.OFFSIDE_FAVOR),
                        contraValue = draftValue(currentDraft, StatKey.OFFSIDE_CONTRA),
                        favorAccent = teamColor,
                        favorLight = teamColorLight,
                        contraAccent = ErrorRed,
                        contraLight = ErrorRedLight,
                        leftLabel = "Contra",
                        rightLabel = "Favor",
                        onFavorIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.OFFSIDE_FAVOR
                                )
                            )
                        },
                        onFavorDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.OFFSIDE_FAVOR
                                )
                            )
                        },
                        onContraIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.OFFSIDE_CONTRA
                                )
                            )
                        },
                        onContraDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.OFFSIDE_CONTRA
                                )
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompactFavorContraCard(
                        modifier = Modifier.weight(1f),
                        title = "Corner",
                        favorValue = draftValue(currentDraft, StatKey.CORNER_POS),
                        contraValue = draftValue(currentDraft, StatKey.CORNER_NEG),
                        favorAccent = teamColor,
                        favorLight = teamColorLight,
                        contraAccent = ErrorRed,
                        contraLight = ErrorRedLight,
                        leftLabel = "Contra",
                        rightLabel = "Favor",
                        onFavorIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.CORNER_POS
                                )
                            )
                        },
                        onFavorDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.CORNER_POS
                                )
                            )
                        },
                        onContraIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.CORNER_NEG
                                )
                            )
                        },
                        onContraDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.CORNER_NEG
                                )
                            )
                        }
                    )

                    CompactFavorContraCard(
                        modifier = Modifier.weight(1f),
                        title = "Falta",
                        favorValue = draftValue(currentDraft, StatKey.FALTA_FAVOR),
                        contraValue = draftValue(currentDraft, StatKey.FALTA_CONTRA),
                        favorAccent = teamColor,
                        favorLight = teamColorLight,
                        contraAccent = ErrorRed,
                        contraLight = ErrorRedLight,
                        leftLabel = "Contra",
                        rightLabel = "Favor",
                        onFavorIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.FALTA_FAVOR
                                )
                            )
                        },
                        onFavorDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.FALTA_FAVOR
                                )
                            )
                        },
                        onContraIncrease = {
                            vm.updatePlayerStatsDraft(
                                increaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.FALTA_CONTRA
                                )
                            )
                        },
                        onContraDecrease = {
                            vm.updatePlayerStatsDraft(
                                decreaseStat(
                                    vm.getOrCreatePlayerStatsDraft(player.id),
                                    StatKey.FALTA_CONTRA
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactSingleCounterCard(
    modifier: Modifier = Modifier,
    title: String,
    value: Int,
    accentColor: Color,
    lightColor: Color,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < 170.dp
        val titleFont = if (compact) 12.sp else 14.sp
        val valueFont = if (compact) 18.sp else 22.sp
        val titleMinHeight = if (compact) 36.dp else 44.dp
        val buttonSpacing = if (compact) 4.dp else 6.dp

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = titleMinHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = titleFont,
                        lineHeight = if (compact) 14.sp else 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary
                    )
                }

                Text(
                    text = value.toString(),
                    fontSize = valueFont,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactActionButton(
                        text = "−",
                        accentColor = accentColor,
                        lightColor = lightColor,
                        compact = compact,
                        onClick = onDecrease
                    )

                    CompactActionButton(
                        text = "+",
                        accentColor = accentColor,
                        lightColor = lightColor,
                        compact = compact,
                        onClick = onIncrease
                    )
                }
            }
        }
    }
}
@Composable
private fun StatsContentCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    accentLight: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun CompactFavorContraCard(
    modifier: Modifier = Modifier,
    title: String,
    favorValue: Int,
    contraValue: Int,
    favorAccent: Color,
    favorLight: Color,
    contraAccent: Color,
    contraLight: Color,
    leftLabel: String = "Contra",
    rightLabel: String = "Favor",
    onFavorIncrease: () -> Unit,
    onFavorDecrease: () -> Unit,
    onContraIncrease: () -> Unit,
    onContraDecrease: () -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < 170.dp
        val titleFont = if (compact) 12.sp else 14.sp
        val titleMinHeight = if (compact) 42.dp else 52.dp
        val columnSpacing = if (compact) 6.dp else 8.dp

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = titleMinHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = titleFont,
                        lineHeight = if (compact) 14.sp else 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(columnSpacing)
                ) {
                    CompactCounterColumn(
                        modifier = Modifier.weight(1f),
                        label = leftLabel,
                        value = contraValue,
                        accentColor = contraAccent,
                        lightColor = contraLight,
                        compact = compact,
                        onIncrease = onContraIncrease,
                        onDecrease = onContraDecrease
                    )

                    CompactCounterColumn(
                        modifier = Modifier.weight(1f),
                        label = rightLabel,
                        value = favorValue,
                        accentColor = favorAccent,
                        lightColor = favorLight,
                        compact = compact,
                        onIncrease = onFavorIncrease,
                        onDecrease = onFavorDecrease
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactCounterColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    accentColor: Color,
    lightColor: Color,
    compact: Boolean = false,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    val labelFont = if (compact) 10.sp else 12.sp
    val valueFont = if (compact) 18.sp else 22.sp
    val buttonSpacing = if (compact) 4.dp else 6.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) 16.dp else 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = labelFont,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = value.toString(),
            fontSize = valueFont,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactActionButton(
                text = "−",
                accentColor = accentColor,
                lightColor = lightColor,
                compact = compact,
                onClick = onDecrease
            )

            CompactActionButton(
                text = "+",
                accentColor = accentColor,
                lightColor = lightColor,
                compact = compact,
                onClick = onIncrease
            )
        }
    }
}

@Composable
private fun CompactActionButton(
    text: String,
    accentColor: Color,
    lightColor: Color,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val buttonSize = if (compact) 28.dp else 30.dp
    val fontSize = if (compact) 14.sp else 16.sp
    val corner = if (compact) 7.dp else 8.dp

    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(corner))
            .background(lightColor)
            .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(corner))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize
        )
    }
}

private fun draftValue(draft: PlayerStatsDraft, key: String): Int = when (key) {
    StatKey.GOL_FAVOR -> draft.golFavor
    StatKey.GOL_CONTRA -> draft.golContra
    StatKey.TIRO_ARCO_POS -> draft.tiroAlArcoPositivo
    StatKey.TIRO_ARCO_NEG -> draft.tiroAlArcoNegativo
    StatKey.PART_GOL_FAVOR -> draft.participacionGolFavor
    StatKey.PART_GOL_CONTRA -> draft.participacionGolContra
    StatKey.REMATE12_POS -> draft.remate12Positivo
    StatKey.REMATE12_NEG -> draft.remate12Negativo

    StatKey.BALON_RECUPERADO -> draft.balonRecogidoFavor
    StatKey.BALON_PERDIDO -> draft.balonRecogidoContra
    StatKey.PASES_BUENOS -> draft.pasesBuenos
    StatKey.PASES_MALOS -> draft.pasesMalos
    StatKey.CENTROS_POS -> draft.centrosPositivos
    StatKey.CENTROS_NEG -> draft.centrosNegativos
    StatKey.RECHAZOS_POS -> draft.rechazosPositivos
    StatKey.RECHAZOS_NEG -> draft.rechazosNegativos

    StatKey.FALTA_FAVOR -> draft.faltaFavor
    StatKey.FALTA_CONTRA -> draft.faltaContra
    StatKey.CORNER_POS -> draft.cornerPositivo
    StatKey.CORNER_NEG -> draft.cornerNegativo
    StatKey.OFFSIDE_FAVOR -> draft.tiroLibreFavor
    StatKey.OFFSIDE_CONTRA -> draft.tiroLibreContra
    StatKey.PENAL_FAVOR -> draft.penalFavor
    StatKey.PENAL_CONTRA -> draft.penalContra

    StatKey.AMARILLA -> draft.amarilla
    StatKey.ROJA -> draft.roja
    else -> 0
}

private fun increaseStat(draft: PlayerStatsDraft, key: String): PlayerStatsDraft = when (key) {
    StatKey.GOL_FAVOR -> draft.copy(golFavor = draft.golFavor + 1)
    StatKey.GOL_CONTRA -> draft.copy(golContra = draft.golContra + 1)
    StatKey.TIRO_ARCO_POS -> draft.copy(tiroAlArcoPositivo = draft.tiroAlArcoPositivo + 1)
    StatKey.TIRO_ARCO_NEG -> draft.copy(tiroAlArcoNegativo = draft.tiroAlArcoNegativo + 1)
    StatKey.PART_GOL_FAVOR -> draft.copy(participacionGolFavor = draft.participacionGolFavor + 1)
    StatKey.PART_GOL_CONTRA -> draft.copy(participacionGolContra = draft.participacionGolContra + 1)
    StatKey.REMATE12_POS -> draft.copy(remate12Positivo = draft.remate12Positivo + 1)
    StatKey.REMATE12_NEG -> draft.copy(remate12Negativo = draft.remate12Negativo + 1)

    StatKey.BALON_RECUPERADO -> draft.copy(balonRecogidoFavor = draft.balonRecogidoFavor + 1)
    StatKey.BALON_PERDIDO -> draft.copy(balonRecogidoContra = draft.balonRecogidoContra + 1)
    StatKey.PASES_BUENOS -> draft.copy(pasesBuenos = draft.pasesBuenos + 1)
    StatKey.PASES_MALOS -> draft.copy(pasesMalos = draft.pasesMalos + 1)
    StatKey.CENTROS_POS -> draft.copy(centrosPositivos = draft.centrosPositivos + 1)
    StatKey.CENTROS_NEG -> draft.copy(centrosNegativos = draft.centrosNegativos + 1)
    StatKey.RECHAZOS_POS -> draft.copy(rechazosPositivos = draft.rechazosPositivos + 1)
    StatKey.RECHAZOS_NEG -> draft.copy(rechazosNegativos = draft.rechazosNegativos + 1)

    StatKey.FALTA_FAVOR -> draft.copy(faltaFavor = draft.faltaFavor + 1)
    StatKey.FALTA_CONTRA -> draft.copy(faltaContra = draft.faltaContra + 1)
    StatKey.CORNER_POS -> draft.copy(cornerPositivo = draft.cornerPositivo + 1)
    StatKey.CORNER_NEG -> draft.copy(cornerNegativo = draft.cornerNegativo + 1)
    StatKey.OFFSIDE_FAVOR -> draft.copy(tiroLibreFavor = draft.tiroLibreFavor + 1)
    StatKey.OFFSIDE_CONTRA -> draft.copy(tiroLibreContra = draft.tiroLibreContra + 1)
    StatKey.PENAL_FAVOR -> draft.copy(penalFavor = draft.penalFavor + 1)
    StatKey.PENAL_CONTRA -> draft.copy(penalContra = draft.penalContra + 1)

    StatKey.AMARILLA -> draft.copy(amarilla = (draft.amarilla + 1).coerceAtMost(2))
    StatKey.ROJA -> draft.copy(roja = (draft.roja + 1).coerceAtMost(1))
    else -> draft
}

private fun decreaseStat(draft: PlayerStatsDraft, key: String): PlayerStatsDraft = when (key) {
    StatKey.GOL_FAVOR -> draft.copy(golFavor = (draft.golFavor - 1).coerceAtLeast(0))
    StatKey.GOL_CONTRA -> draft.copy(golContra = (draft.golContra - 1).coerceAtLeast(0))
    StatKey.TIRO_ARCO_POS -> draft.copy(tiroAlArcoPositivo = (draft.tiroAlArcoPositivo - 1).coerceAtLeast(0))
    StatKey.TIRO_ARCO_NEG -> draft.copy(tiroAlArcoNegativo = (draft.tiroAlArcoNegativo - 1).coerceAtLeast(0))
    StatKey.PART_GOL_FAVOR -> draft.copy(participacionGolFavor = (draft.participacionGolFavor - 1).coerceAtLeast(0))
    StatKey.PART_GOL_CONTRA -> draft.copy(participacionGolContra = (draft.participacionGolContra - 1).coerceAtLeast(0))
    StatKey.REMATE12_POS -> draft.copy(remate12Positivo = (draft.remate12Positivo - 1).coerceAtLeast(0))
    StatKey.REMATE12_NEG -> draft.copy(remate12Negativo = (draft.remate12Negativo - 1).coerceAtLeast(0))

    StatKey.BALON_RECUPERADO -> draft.copy(balonRecogidoFavor = (draft.balonRecogidoFavor - 1).coerceAtLeast(0))
    StatKey.BALON_PERDIDO -> draft.copy(balonRecogidoContra = (draft.balonRecogidoContra - 1).coerceAtLeast(0))
    StatKey.PASES_BUENOS -> draft.copy(pasesBuenos = (draft.pasesBuenos - 1).coerceAtLeast(0))
    StatKey.PASES_MALOS -> draft.copy(pasesMalos = (draft.pasesMalos - 1).coerceAtLeast(0))
    StatKey.CENTROS_POS -> draft.copy(centrosPositivos = (draft.centrosPositivos - 1).coerceAtLeast(0))
    StatKey.CENTROS_NEG -> draft.copy(centrosNegativos = (draft.centrosNegativos - 1).coerceAtLeast(0))
    StatKey.RECHAZOS_POS -> draft.copy(rechazosPositivos = (draft.rechazosPositivos - 1).coerceAtLeast(0))
    StatKey.RECHAZOS_NEG -> draft.copy(rechazosNegativos = (draft.rechazosNegativos - 1).coerceAtLeast(0))

    StatKey.FALTA_FAVOR -> draft.copy(faltaFavor = (draft.faltaFavor - 1).coerceAtLeast(0))
    StatKey.FALTA_CONTRA -> draft.copy(faltaContra = (draft.faltaContra - 1).coerceAtLeast(0))
    StatKey.CORNER_POS -> draft.copy(cornerPositivo = (draft.cornerPositivo - 1).coerceAtLeast(0))
    StatKey.CORNER_NEG -> draft.copy(cornerNegativo = (draft.cornerNegativo - 1).coerceAtLeast(0))
    StatKey.OFFSIDE_FAVOR -> draft.copy(tiroLibreFavor = (draft.tiroLibreFavor - 1).coerceAtLeast(0))
    StatKey.OFFSIDE_CONTRA -> draft.copy(tiroLibreContra = (draft.tiroLibreContra - 1).coerceAtLeast(0))
    StatKey.PENAL_FAVOR -> draft.copy(penalFavor = (draft.penalFavor - 1).coerceAtLeast(0))
    StatKey.PENAL_CONTRA -> draft.copy(penalContra = (draft.penalContra - 1).coerceAtLeast(0))

    StatKey.AMARILLA -> draft.copy(amarilla = (draft.amarilla - 1).coerceAtLeast(0))
    StatKey.ROJA -> draft.copy(roja = (draft.roja - 1).coerceAtLeast(0))
    else -> draft
}