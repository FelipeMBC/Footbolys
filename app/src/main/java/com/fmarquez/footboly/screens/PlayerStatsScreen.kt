package com.fmarquez.footboly.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import com.fmarquez.footboly.modelos.PlayerStatsDraft
import com.fmarquez.footboly.vm.FutbolViewModel

private val BgColor          = Color(0xFFF7F7F5)
private val SurfaceColor     = Color(0xFFFFFFFF)
private val AccentGreen      = Color(0xFF1E6B45)
private val AccentGreenLight = Color(0xFFE8F2EC)
private val TextPrimary      = Color(0xFF111111)
private val TextSecondary    = Color(0xFF888888)
private val BorderColor      = Color(0xFFE0E0DC)

data class SingleStatUi(val label: String, val icon: ImageVector, val initialValue: Int = 0)
data class DualStatUi(
    val label: String,
    val favorLabel: String, val contraLabel: String,
    val favorIcon: ImageVector, val contraIcon: ImageVector,
    val favorInitial: Int = 0, val contraInitial: Int = 0
)

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

    val singleStats = listOf(
        SingleStatUi("Gol", Icons.Default.SportsSoccer),
        SingleStatUi("Asistencia", Icons.Default.Send),
        SingleStatUi("Amarilla", Icons.Default.Warning),
        SingleStatUi("Roja", Icons.Default.Dangerous),
        SingleStatUi("Disparos al Arco", Icons.Default.Star),
        SingleStatUi("Ocasiones de Gol", Icons.Default.Star),
        SingleStatUi("Pelotas Perdidas", Icons.Default.Clear),
        SingleStatUi("Pelotas Recuperadas", Icons.Default.Star),
        SingleStatUi("Centros Buenos", Icons.Default.Check),
        SingleStatUi("Centros Malos", Icons.Default.Clear)
    )

    val dualStats = listOf(
        DualStatUi("Faltas", "Falta a Favor", "Falta en Contra", Icons.Default.CheckCircle, Icons.Default.Close),
        DualStatUi("Corner", "Corner a Favor", "Corner en Contra", Icons.Default.CheckCircle, Icons.Default.Close),
        DualStatUi("Tiro Libre", "Tiro Libre a Favor", "Tiro Libre en Contra", Icons.Default.CheckCircle, Icons.Default.Close),
        DualStatUi("T.L. Lateral", "Tiro Libre Lateral a Favor", "Tiro Libre Lateral en Contra", Icons.Default.CheckCircle, Icons.Default.Close)
    )

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
                                Toast.makeText(context, if (savedLines.isEmpty()) "No hubo cambios" else "Cambios guardados", Toast.LENGTH_SHORT).show()
                                navHostController.popBackStack()
                            } else {
                                val toastText = if (savedLines.isEmpty()) "Sin cambios para registrar"
                                else (savedLines + "Registrado").joinToString("\n")
                                Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar", tint = Color.White, modifier = Modifier.size(18.dp))
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
        ) {
            // ── Tarjeta del jugador ───────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Emoji del equipo en círculo
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(AccentGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(team.logoEmoji, fontSize = 26.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(player.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                        Text("N° ${player.number}", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isEditingFinishedMatch) Color(0xFFFFF3E0) else AccentGreenLight)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isEditingFinishedMatch) "Edición" else "En curso",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isEditingFinishedMatch) Color(0xFFE65100) else AccentGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Registro estadístico", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(singleStats) { stat ->
                    val currentDraft = vm.getOrCreatePlayerStatsDraft(player.id)
                    SingleStatCard(
                        stat = stat,
                        value = singleStatValue(currentDraft, stat.label),
                        onIncrease = { vm.updatePlayerStatsDraft(increaseSingleStat(vm.getOrCreatePlayerStatsDraft(player.id), stat.label)) },
                        onDecrease = { vm.updatePlayerStatsDraft(decreaseSingleStat(vm.getOrCreatePlayerStatsDraft(player.id), stat.label)) }
                    )
                }
                items(dualStats) { stat ->
                    val currentDraft = vm.getOrCreatePlayerStatsDraft(player.id)
                    DualStatCard(
                        stat = stat,
                        favorValue = dualFavorValue(currentDraft, stat.favorLabel),
                        contraValue = dualContraValue(currentDraft, stat.contraLabel),
                        onFavorIncrease = { vm.updatePlayerStatsDraft(increaseDualFavorStat(vm.getOrCreatePlayerStatsDraft(player.id), stat.favorLabel)) },
                        onFavorDecrease = { vm.updatePlayerStatsDraft(decreaseDualFavorStat(vm.getOrCreatePlayerStatsDraft(player.id), stat.favorLabel)) },
                        onContraIncrease = { vm.updatePlayerStatsDraft(increaseDualContraStat(vm.getOrCreatePlayerStatsDraft(player.id), stat.contraLabel)) },
                        onContraDecrease = { vm.updatePlayerStatsDraft(decreaseDualContraStat(vm.getOrCreatePlayerStatsDraft(player.id), stat.contraLabel)) }
                    )
                }
            }
        }
    }
}

// ── Funciones de estado (sin cambios) ────────────────────────────────────────
fun singleStatValue(draft: PlayerStatsDraft, label: String) = when (label) {
    "Gol" -> draft.gol; "Asistencia" -> draft.asistencia; "Amarilla" -> draft.amarilla
    "Roja" -> draft.roja; "Disparos al Arco" -> draft.disparosAlArco
    "Ocasiones de Gol" -> draft.ocasionesDeGol; "Pelotas Perdidas" -> draft.pelotasPerdidas
    "Pelotas Recuperadas" -> draft.pelotasRecuperadas; "Centros Buenos" -> draft.centrosBuenos
    "Centros Malos" -> draft.centrosMalos; else -> 0
}

fun dualFavorValue(draft: PlayerStatsDraft, label: String) = when (label) {
    "Falta a Favor" -> draft.faltaAFavor; "Corner a Favor" -> draft.cornerAFavor
    "Tiro Libre a Favor" -> draft.tiroLibreAFavor; "Tiro Libre Lateral a Favor" -> draft.tiroLibreLateralAFavor; else -> 0
}

fun dualContraValue(draft: PlayerStatsDraft, label: String) = when (label) {
    "Falta en Contra" -> draft.faltaEnContra; "Corner en Contra" -> draft.cornerEnContra
    "Tiro Libre en Contra" -> draft.tiroLibreEnContra; "Tiro Libre Lateral en Contra" -> draft.tiroLibreLateralEnContra; else -> 0
}

fun increaseSingleStat(draft: PlayerStatsDraft, label: String) = when (label) {
    "Gol" -> draft.copy(gol = draft.gol + 1); "Asistencia" -> draft.copy(asistencia = draft.asistencia + 1)
    "Amarilla" -> draft.copy(amarilla = draft.amarilla + 1); "Roja" -> draft.copy(roja = draft.roja + 1)
    "Disparos al Arco" -> draft.copy(disparosAlArco = draft.disparosAlArco + 1)
    "Ocasiones de Gol" -> draft.copy(ocasionesDeGol = draft.ocasionesDeGol + 1)
    "Pelotas Perdidas" -> draft.copy(pelotasPerdidas = draft.pelotasPerdidas + 1)
    "Pelotas Recuperadas" -> draft.copy(pelotasRecuperadas = draft.pelotasRecuperadas + 1)
    "Centros Buenos" -> draft.copy(centrosBuenos = draft.centrosBuenos + 1)
    "Centros Malos" -> draft.copy(centrosMalos = draft.centrosMalos + 1); else -> draft
}

fun decreaseSingleStat(draft: PlayerStatsDraft, label: String) = when (label) {
    "Gol" -> draft.copy(gol = (draft.gol - 1).coerceAtLeast(0))
    "Asistencia" -> draft.copy(asistencia = (draft.asistencia - 1).coerceAtLeast(0))
    "Amarilla" -> draft.copy(amarilla = (draft.amarilla - 1).coerceAtLeast(0))
    "Roja" -> draft.copy(roja = (draft.roja - 1).coerceAtLeast(0))
    "Disparos al Arco" -> draft.copy(disparosAlArco = (draft.disparosAlArco - 1).coerceAtLeast(0))
    "Ocasiones de Gol" -> draft.copy(ocasionesDeGol = (draft.ocasionesDeGol - 1).coerceAtLeast(0))
    "Pelotas Perdidas" -> draft.copy(pelotasPerdidas = (draft.pelotasPerdidas - 1).coerceAtLeast(0))
    "Pelotas Recuperadas" -> draft.copy(pelotasRecuperadas = (draft.pelotasRecuperadas - 1).coerceAtLeast(0))
    "Centros Buenos" -> draft.copy(centrosBuenos = (draft.centrosBuenos - 1).coerceAtLeast(0))
    "Centros Malos" -> draft.copy(centrosMalos = (draft.centrosMalos - 1).coerceAtLeast(0)); else -> draft
}

fun increaseDualFavorStat(draft: PlayerStatsDraft, label: String) = when (label) {
    "Falta a Favor" -> draft.copy(faltaAFavor = draft.faltaAFavor + 1)
    "Corner a Favor" -> draft.copy(cornerAFavor = draft.cornerAFavor + 1)
    "Tiro Libre a Favor" -> draft.copy(tiroLibreAFavor = draft.tiroLibreAFavor + 1)
    "Tiro Libre Lateral a Favor" -> draft.copy(tiroLibreLateralAFavor = draft.tiroLibreLateralAFavor + 1); else -> draft
}

fun decreaseDualFavorStat(draft: PlayerStatsDraft, label: String) = when (label) {
    "Falta a Favor" -> draft.copy(faltaAFavor = (draft.faltaAFavor - 1).coerceAtLeast(0))
    "Corner a Favor" -> draft.copy(cornerAFavor = (draft.cornerAFavor - 1).coerceAtLeast(0))
    "Tiro Libre a Favor" -> draft.copy(tiroLibreAFavor = (draft.tiroLibreAFavor - 1).coerceAtLeast(0))
    "Tiro Libre Lateral a Favor" -> draft.copy(tiroLibreLateralAFavor = (draft.tiroLibreLateralAFavor - 1).coerceAtLeast(0)); else -> draft
}

fun increaseDualContraStat(draft: PlayerStatsDraft, label: String) = when (label) {
    "Falta en Contra" -> draft.copy(faltaEnContra = draft.faltaEnContra + 1)
    "Corner en Contra" -> draft.copy(cornerEnContra = draft.cornerEnContra + 1)
    "Tiro Libre en Contra" -> draft.copy(tiroLibreEnContra = draft.tiroLibreEnContra + 1)
    "Tiro Libre Lateral en Contra" -> draft.copy(tiroLibreLateralEnContra = draft.tiroLibreLateralEnContra + 1); else -> draft
}

fun decreaseDualContraStat(draft: PlayerStatsDraft, label: String) = when (label) {
    "Falta en Contra" -> draft.copy(faltaEnContra = (draft.faltaEnContra - 1).coerceAtLeast(0))
    "Corner en Contra" -> draft.copy(cornerEnContra = (draft.cornerEnContra - 1).coerceAtLeast(0))
    "Tiro Libre en Contra" -> draft.copy(tiroLibreEnContra = (draft.tiroLibreEnContra - 1).coerceAtLeast(0))
    "Tiro Libre Lateral en Contra" -> draft.copy(tiroLibreLateralEnContra = (draft.tiroLibreLateralEnContra - 1).coerceAtLeast(0)); else -> draft
}

// ── Componentes de tarjetas de estadísticas ───────────────────────────────────
@Composable
fun SingleStatCard(stat: SingleStatUi, value: Int, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(AccentGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(stat.icon, contentDescription = stat.label, tint = AccentGreen, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(stat.label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(6.dp))

            Text(value.toString(), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = TextPrimary)

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.Center) {
                OutlinedButton(
                    onClick = onIncrease,
                    modifier = Modifier.size(width = 48.dp, height = 36.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) { Text("+", fontWeight = FontWeight.Bold) }

                Spacer(modifier = Modifier.width(6.dp))

                OutlinedButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(width = 48.dp, height = 36.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) { Text("−", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun DualStatCard(
    stat: DualStatUi,
    favorValue: Int, contraValue: Int,
    onFavorIncrease: () -> Unit, onFavorDecrease: () -> Unit,
    onContraIncrease: () -> Unit, onContraDecrease: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stat.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // A favor
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("A favor", fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(favorValue.toString(), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        OutlinedButton(
                            onClick = onFavorIncrease,
                            modifier = Modifier.size(width = 38.dp, height = 32.dp),
                            shape = RoundedCornerShape(7.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { Text("+", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedButton(
                            onClick = onFavorDecrease,
                            modifier = Modifier.size(width = 38.dp, height = 32.dp),
                            shape = RoundedCornerShape(7.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { Text("−", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                // Divisor
                Box(modifier = Modifier.width(1.dp).height(80.dp).background(BorderColor).align(Alignment.CenterVertically))

                // En contra
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("En contra", fontSize = 11.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(contraValue.toString(), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        OutlinedButton(
                            onClick = onContraIncrease,
                            modifier = Modifier.size(width = 38.dp, height = 32.dp),
                            shape = RoundedCornerShape(7.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD32F2F)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { Text("+", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedButton(
                            onClick = onContraDecrease,
                            modifier = Modifier.size(width = 38.dp, height = 32.dp),
                            shape = RoundedCornerShape(7.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) { Text("−", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}