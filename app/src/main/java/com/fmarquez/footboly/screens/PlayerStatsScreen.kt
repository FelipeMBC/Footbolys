package com.fmarquez.footboly.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.modelos.PlayerStatsDraft
import com.fmarquez.footboly.util.hexToColor
import com.fmarquez.footboly.util.teamColorLight
import com.fmarquez.footboly.vm.FutbolViewModel

private val BgColor = Color(0xFFF7F7F5)
private val SurfaceColor = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF111111)
private val TextSecondary = Color(0xFF888888)
private val BorderColor = Color(0xFFE0E0DC)
private val ErrorRed = Color(0xFFD32F2F)
private val ErrorRedLight = Color(0xFFFFF1F1)

private object StatKey {
    const val GOL_FAVOR = "GOL_FAVOR"
    const val GOL_CONTRA = "GOL_CONTRA"
    const val TIRO_ARCO_POS = "TIRO_ARCO_POS"
    const val TIRO_ARCO_NEG = "TIRO_ARCO_NEG"
    const val PART_GOL_FAVOR = "PART_GOL_FAVOR"
    const val PART_GOL_CONTRA = "PART_GOL_CONTRA"
    const val REMATE12_POS = "REMATE12_POS"
    const val REMATE12_NEG = "REMATE12_NEG"

    const val BALON_RECOGIDO_FAVOR = "BALON_RECOGIDO_FAVOR"
    const val BALON_RECOGIDO_CONTRA = "BALON_RECOGIDO_CONTRA"
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
    const val TIRO_LIBRE_FAVOR = "TIRO_LIBRE_FAVOR"
    const val TIRO_LIBRE_CONTRA = "TIRO_LIBRE_CONTRA"
    const val PENAL_FAVOR = "PENAL_FAVOR"
    const val PENAL_CONTRA = "PENAL_CONTRA"

    const val AMARILLA = "AMARILLA"
    const val ROJA = "ROJA"
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

    var expandGol by rememberSaveable { mutableStateOf(true) }
    var expandJuego by rememberSaveable { mutableStateOf(false) }
    var expandDetenido by rememberSaveable { mutableStateOf(false) }
    var expandTarjetas by rememberSaveable { mutableStateOf(false) }

    val currentDraft = vm.getOrCreatePlayerStatsDraft(player.id)

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
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(teamColorLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(team.logoEmoji, fontSize = 26.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
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
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            ExpandableStatsSection(
                title = "Gol",
                icon = Icons.Default.SportsSoccer,
                expanded = expandGol,
                accentColor = teamColor,
                accentLight = teamColorLight,
                onToggle = { expandGol = !expandGol }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FavorContraRow(
                        title = "Gol",
                        favorValue = draftValue(currentDraft, StatKey.GOL_FAVOR),
                        contraValue = draftValue(currentDraft, StatKey.GOL_CONTRA),
                        accentColor = teamColor,
                        accentLight = teamColorLight,
                        onFavorIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.GOL_FAVOR)) },
                        onFavorDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.GOL_FAVOR)) },
                        onContraIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.GOL_CONTRA)) },
                        onContraDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.GOL_CONTRA)) }
                    )

                    PositiveNegativeRow(
                        title = "Tiro al arco",
                        positiveValue = draftValue(currentDraft, StatKey.TIRO_ARCO_POS),
                        negativeValue = draftValue(currentDraft, StatKey.TIRO_ARCO_NEG),
                        positiveAccent = teamColor,
                        positiveLight = teamColorLight,
                        onPositiveIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.TIRO_ARCO_POS)) },
                        onPositiveDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.TIRO_ARCO_POS)) },
                        onNegativeIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.TIRO_ARCO_NEG)) },
                        onNegativeDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.TIRO_ARCO_NEG)) }
                    )

                    FavorContraRow(
                        title = "Participación de gol",
                        favorValue = draftValue(currentDraft, StatKey.PART_GOL_FAVOR),
                        contraValue = draftValue(currentDraft, StatKey.PART_GOL_CONTRA),
                        accentColor = teamColor,
                        accentLight = teamColorLight,
                        onFavorIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.PART_GOL_FAVOR)) },
                        onFavorDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.PART_GOL_FAVOR)) },
                        onContraIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.PART_GOL_CONTRA)) },
                        onContraDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.PART_GOL_CONTRA)) }
                    )

                    PositiveNegativeRow(
                        title = "Remate 1/2",
                        positiveValue = draftValue(currentDraft, StatKey.REMATE12_POS),
                        negativeValue = draftValue(currentDraft, StatKey.REMATE12_NEG),
                        positiveAccent = teamColor,
                        positiveLight = teamColorLight,
                        onPositiveIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.REMATE12_POS)) },
                        onPositiveDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.REMATE12_POS)) },
                        onNegativeIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.REMATE12_NEG)) },
                        onNegativeDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.REMATE12_NEG)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExpandableStatsSection(
                title = "Recuperación / Juego",
                icon = Icons.Default.Star,
                expanded = expandJuego,
                accentColor = teamColor,
                accentLight = teamColorLight,
                onToggle = { expandJuego = !expandJuego }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FavorContraRow(
                        title = "Balón recogido",
                        favorValue = draftValue(currentDraft, StatKey.BALON_RECOGIDO_FAVOR),
                        contraValue = draftValue(currentDraft, StatKey.BALON_RECOGIDO_CONTRA),
                        accentColor = teamColor,
                        accentLight = teamColorLight,
                        onFavorIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.BALON_RECOGIDO_FAVOR)) },
                        onFavorDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.BALON_RECOGIDO_FAVOR)) },
                        onContraIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.BALON_RECOGIDO_CONTRA)) },
                        onContraDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.BALON_RECOGIDO_CONTRA)) }
                    )

                    GoodBadRow(
                        title = "Pases",
                        positiveText = "Buenos",
                        negativeText = "Malos",
                        positiveValue = draftValue(currentDraft, StatKey.PASES_BUENOS),
                        negativeValue = draftValue(currentDraft, StatKey.PASES_MALOS),
                        positiveAccent = teamColor,
                        positiveLight = teamColorLight,
                        onPositiveIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.PASES_BUENOS)) },
                        onPositiveDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.PASES_BUENOS)) },
                        onNegativeIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.PASES_MALOS)) },
                        onNegativeDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.PASES_MALOS)) }
                    )

                    PositiveNegativeRow(
                        title = "Centros",
                        positiveValue = draftValue(currentDraft, StatKey.CENTROS_POS),
                        negativeValue = draftValue(currentDraft, StatKey.CENTROS_NEG),
                        positiveAccent = teamColor,
                        positiveLight = teamColorLight,
                        onPositiveIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.CENTROS_POS)) },
                        onPositiveDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.CENTROS_POS)) },
                        onNegativeIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.CENTROS_NEG)) },
                        onNegativeDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.CENTROS_NEG)) }
                    )

                    PositiveNegativeRow(
                        title = "Rechazos",
                        positiveValue = draftValue(currentDraft, StatKey.RECHAZOS_POS),
                        negativeValue = draftValue(currentDraft, StatKey.RECHAZOS_NEG),
                        positiveAccent = teamColor,
                        positiveLight = teamColorLight,
                        onPositiveIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.RECHAZOS_POS)) },
                        onPositiveDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.RECHAZOS_POS)) },
                        onNegativeIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.RECHAZOS_NEG)) },
                        onNegativeDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.RECHAZOS_NEG)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExpandableStatsSection(
                title = "Faltas / Balón detenido",
                icon = Icons.Default.Star,
                expanded = expandDetenido,
                accentColor = teamColor,
                accentLight = teamColorLight,
                onToggle = { expandDetenido = !expandDetenido }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FavorContraRow(
                        title = "Falta",
                        favorValue = draftValue(currentDraft, StatKey.FALTA_FAVOR),
                        contraValue = draftValue(currentDraft, StatKey.FALTA_CONTRA),
                        accentColor = teamColor,
                        accentLight = teamColorLight,
                        onFavorIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.FALTA_FAVOR)) },
                        onFavorDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.FALTA_FAVOR)) },
                        onContraIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.FALTA_CONTRA)) },
                        onContraDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.FALTA_CONTRA)) }
                    )

                    PositiveNegativeRow(
                        title = "Corner",
                        positiveValue = draftValue(currentDraft, StatKey.CORNER_POS),
                        negativeValue = draftValue(currentDraft, StatKey.CORNER_NEG),
                        positiveAccent = teamColor,
                        positiveLight = teamColorLight,
                        onPositiveIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.CORNER_POS)) },
                        onPositiveDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.CORNER_POS)) },
                        onNegativeIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.CORNER_NEG)) },
                        onNegativeDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.CORNER_NEG)) }
                    )

                    FavorContraRow(
                        title = "Tiro libre",
                        favorValue = draftValue(currentDraft, StatKey.TIRO_LIBRE_FAVOR),
                        contraValue = draftValue(currentDraft, StatKey.TIRO_LIBRE_CONTRA),
                        accentColor = teamColor,
                        accentLight = teamColorLight,
                        onFavorIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.TIRO_LIBRE_FAVOR)) },
                        onFavorDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.TIRO_LIBRE_FAVOR)) },
                        onContraIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.TIRO_LIBRE_CONTRA)) },
                        onContraDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.TIRO_LIBRE_CONTRA)) }
                    )

                    FavorContraRow(
                        title = "Penal",
                        favorValue = draftValue(currentDraft, StatKey.PENAL_FAVOR),
                        contraValue = draftValue(currentDraft, StatKey.PENAL_CONTRA),
                        accentColor = teamColor,
                        accentLight = teamColorLight,
                        onFavorIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.PENAL_FAVOR)) },
                        onFavorDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.PENAL_FAVOR)) },
                        onContraIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.PENAL_CONTRA)) },
                        onContraDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.PENAL_CONTRA)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ExpandableStatsSection(
                title = "Tarjetas",
                icon = Icons.Default.Star,
                expanded = expandTarjetas,
                accentColor = teamColor,
                accentLight = teamColorLight,
                onToggle = { expandTarjetas = !expandTarjetas }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SingleCounterRow(
                        title = "Amarilla",
                        value = draftValue(currentDraft, StatKey.AMARILLA),
                        accentColor = Color(0xFFF2B705),
                        lightColor = Color(0xFFFFF8DD),
                        onIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.AMARILLA)) },
                        onDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.AMARILLA)) }
                    )

                    SingleCounterRow(
                        title = "Roja",
                        value = draftValue(currentDraft, StatKey.ROJA),
                        accentColor = ErrorRed,
                        lightColor = ErrorRedLight,
                        onIncrease = { vm.updatePlayerStatsDraft(increaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.ROJA)) },
                        onDecrease = { vm.updatePlayerStatsDraft(decreaseStat(vm.getOrCreatePlayerStatsDraft(player.id), StatKey.ROJA)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ExpandableStatsSection(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    accentColor: Color,
    accentLight: Color,
    onToggle: () -> Unit,
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
                .clickable { onToggle() }
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
                Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary
            )
        }

        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun FavorContraRow(
    title: String,
    favorValue: Int,
    contraValue: Int,
    accentColor: Color,
    accentLight: Color,
    onFavorIncrease: () -> Unit,
    onFavorDecrease: () -> Unit,
    onContraIncrease: () -> Unit,
    onContraDecrease: () -> Unit
) {
    BaseStatCard(title = title) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CounterCard(
                modifier = Modifier.weight(1f),
                label = "Favor",
                value = favorValue,
                accentColor = accentColor,
                lightColor = accentLight,
                onIncrease = onFavorIncrease,
                onDecrease = onFavorDecrease
            )

            CounterCard(
                modifier = Modifier.weight(1f),
                label = "Contra",
                value = contraValue,
                accentColor = ErrorRed,
                lightColor = ErrorRedLight,
                onIncrease = onContraIncrease,
                onDecrease = onContraDecrease
            )
        }
    }
}

@Composable
private fun PositiveNegativeRow(
    title: String,
    positiveValue: Int,
    negativeValue: Int,
    positiveAccent: Color,
    positiveLight: Color,
    onPositiveIncrease: () -> Unit,
    onPositiveDecrease: () -> Unit,
    onNegativeIncrease: () -> Unit,
    onNegativeDecrease: () -> Unit
) {
    BaseStatCard(title = title) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CounterCard(
                modifier = Modifier.weight(1f),
                label = "+",
                value = positiveValue,
                accentColor = positiveAccent,
                lightColor = positiveLight,
                onIncrease = onPositiveIncrease,
                onDecrease = onPositiveDecrease
            )

            CounterCard(
                modifier = Modifier.weight(1f),
                label = "-",
                value = negativeValue,
                accentColor = ErrorRed,
                lightColor = ErrorRedLight,
                onIncrease = onNegativeIncrease,
                onDecrease = onNegativeDecrease
            )
        }
    }
}

@Composable
private fun GoodBadRow(
    title: String,
    positiveText: String,
    negativeText: String,
    positiveValue: Int,
    negativeValue: Int,
    positiveAccent: Color,
    positiveLight: Color,
    onPositiveIncrease: () -> Unit,
    onPositiveDecrease: () -> Unit,
    onNegativeIncrease: () -> Unit,
    onNegativeDecrease: () -> Unit
) {
    BaseStatCard(title = title) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CounterCard(
                modifier = Modifier.weight(1f),
                label = positiveText,
                value = positiveValue,
                accentColor = positiveAccent,
                lightColor = positiveLight,
                onIncrease = onPositiveIncrease,
                onDecrease = onPositiveDecrease
            )

            CounterCard(
                modifier = Modifier.weight(1f),
                label = negativeText,
                value = negativeValue,
                accentColor = ErrorRed,
                lightColor = ErrorRedLight,
                onIncrease = onNegativeIncrease,
                onDecrease = onNegativeDecrease
            )
        }
    }
}

@Composable
private fun SingleCounterRow(
    title: String,
    value: Int,
    accentColor: Color,
    lightColor: Color,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    BaseStatCard(title = title) {
        CounterCard(
            modifier = Modifier.fillMaxWidth(),
            label = title,
            value = value,
            accentColor = accentColor,
            lightColor = lightColor,
            onIncrease = onIncrease,
            onDecrease = onDecrease
        )
    }
}

@Composable
private fun BaseStatCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun CounterCard(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    accentColor: Color,
    lightColor: Color,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(lightColor)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDecrease,
                modifier = Modifier.size(width = 48.dp, height = 36.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, BorderColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("−", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedButton(
                onClick = onIncrease,
                modifier = Modifier.size(width = 48.dp, height = 36.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, accentColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+", fontWeight = FontWeight.Bold)
            }
        }
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

    StatKey.BALON_RECOGIDO_FAVOR -> draft.balonRecogidoFavor
    StatKey.BALON_RECOGIDO_CONTRA -> draft.balonRecogidoContra
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
    StatKey.TIRO_LIBRE_FAVOR -> draft.tiroLibreFavor
    StatKey.TIRO_LIBRE_CONTRA -> draft.tiroLibreContra
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

    StatKey.BALON_RECOGIDO_FAVOR -> draft.copy(balonRecogidoFavor = draft.balonRecogidoFavor + 1)
    StatKey.BALON_RECOGIDO_CONTRA -> draft.copy(balonRecogidoContra = draft.balonRecogidoContra + 1)
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
    StatKey.TIRO_LIBRE_FAVOR -> draft.copy(tiroLibreFavor = draft.tiroLibreFavor + 1)
    StatKey.TIRO_LIBRE_CONTRA -> draft.copy(tiroLibreContra = draft.tiroLibreContra + 1)
    StatKey.PENAL_FAVOR -> draft.copy(penalFavor = draft.penalFavor + 1)
    StatKey.PENAL_CONTRA -> draft.copy(penalContra = draft.penalContra + 1)

    StatKey.AMARILLA -> draft.copy(amarilla = draft.amarilla + 1)
    StatKey.ROJA -> draft.copy(roja = draft.roja + 1)
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

    StatKey.BALON_RECOGIDO_FAVOR -> draft.copy(balonRecogidoFavor = (draft.balonRecogidoFavor - 1).coerceAtLeast(0))
    StatKey.BALON_RECOGIDO_CONTRA -> draft.copy(balonRecogidoContra = (draft.balonRecogidoContra - 1).coerceAtLeast(0))
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
    StatKey.TIRO_LIBRE_FAVOR -> draft.copy(tiroLibreFavor = (draft.tiroLibreFavor - 1).coerceAtLeast(0))
    StatKey.TIRO_LIBRE_CONTRA -> draft.copy(tiroLibreContra = (draft.tiroLibreContra - 1).coerceAtLeast(0))
    StatKey.PENAL_FAVOR -> draft.copy(penalFavor = (draft.penalFavor - 1).coerceAtLeast(0))
    StatKey.PENAL_CONTRA -> draft.copy(penalContra = (draft.penalContra - 1).coerceAtLeast(0))

    StatKey.AMARILLA -> draft.copy(amarilla = (draft.amarilla - 1).coerceAtLeast(0))
    StatKey.ROJA -> draft.copy(roja = (draft.roja - 1).coerceAtLeast(0))
    else -> draft
}