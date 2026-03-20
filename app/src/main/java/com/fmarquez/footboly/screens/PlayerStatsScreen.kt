package com.fmarquez.footboly.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.modelos.PlayerStatsDraft
import com.fmarquez.footboly.vm.FutbolViewModel

data class SingleStatUi(
    val label: String,
    val icon: ImageVector,
    val initialValue: Int = 0
)

data class DualStatUi(
    val label: String,
    val favorLabel: String,
    val contraLabel: String,
    val favorIcon: ImageVector,
    val contraIcon: ImageVector,
    val favorInitial: Int = 0,
    val contraInitial: Int = 0
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
        DualStatUi(
            label = "Faltas",
            favorLabel = "Falta a Favor",
            contraLabel = "Falta en Contra",
            favorIcon = Icons.Default.CheckCircle,
            contraIcon = Icons.Default.Close
        ),
        DualStatUi(
            label = "Corner",
            favorLabel = "Corner a Favor",
            contraLabel = "Corner en Contra",
            favorIcon = Icons.Default.CheckCircle,
            contraIcon = Icons.Default.Close
        ),
        DualStatUi(
            label = "Tiro Libre",
            favorLabel = "Tiro Libre a Favor",
            contraLabel = "Tiro Libre en Contra",
            favorIcon = Icons.Default.CheckCircle,
            contraIcon = Icons.Default.Close
        ),
        DualStatUi(
            label = "Tiro Libre Lateral",
            favorLabel = "Tiro Libre Lateral a Favor",
            contraLabel = "Tiro Libre Lateral en Contra",
            favorIcon = Icons.Default.CheckCircle,
            contraIcon = Icons.Default.Close
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditingFinishedMatch) "Editar estadísticas"
                        else "Estadísticas"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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

                                Toast.makeText(
                                    context,
                                    toastText,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Guardar cambios"
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = team.logoEmoji,
                        fontSize = 72.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "ID Jugador: ${player.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "ID Partido: ${activeMatch.id}",
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isEditingFinishedMatch) "Modo edición de partido finalizado"
                        else "Modo registro en partido en curso",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "N° ${player.number}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = player.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Registro estadístico",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(singleStats) { stat ->
                    val currentDraft = vm.getOrCreatePlayerStatsDraft(player.id)

                    SingleStatCard(
                        stat = stat,
                        value = singleStatValue(currentDraft, stat.label),
                        onIncrease = {
                            val latestDraft = vm.getOrCreatePlayerStatsDraft(player.id)
                            vm.updatePlayerStatsDraft(
                                increaseSingleStat(latestDraft, stat.label)
                            )
                        },
                        onDecrease = {
                            val latestDraft = vm.getOrCreatePlayerStatsDraft(player.id)
                            vm.updatePlayerStatsDraft(
                                decreaseSingleStat(latestDraft, stat.label)
                            )
                        }
                    )
                }

                items(dualStats) { stat ->
                    val currentDraft = vm.getOrCreatePlayerStatsDraft(player.id)

                    DualStatCard(
                        stat = stat,
                        favorValue = dualFavorValue(currentDraft, stat.favorLabel),
                        contraValue = dualContraValue(currentDraft, stat.contraLabel),
                        onFavorIncrease = {
                            val latestDraft = vm.getOrCreatePlayerStatsDraft(player.id)
                            vm.updatePlayerStatsDraft(
                                increaseDualFavorStat(latestDraft, stat.favorLabel)
                            )
                        },
                        onFavorDecrease = {
                            val latestDraft = vm.getOrCreatePlayerStatsDraft(player.id)
                            vm.updatePlayerStatsDraft(
                                decreaseDualFavorStat(latestDraft, stat.favorLabel)
                            )
                        },
                        onContraIncrease = {
                            val latestDraft = vm.getOrCreatePlayerStatsDraft(player.id)
                            vm.updatePlayerStatsDraft(
                                increaseDualContraStat(latestDraft, stat.contraLabel)
                            )
                        },
                        onContraDecrease = {
                            val latestDraft = vm.getOrCreatePlayerStatsDraft(player.id)
                            vm.updatePlayerStatsDraft(
                                decreaseDualContraStat(latestDraft, stat.contraLabel)
                            )
                        }
                    )
                }
            }
        }
    }
}

fun singleStatValue(draft: PlayerStatsDraft, label: String): Int {
    return when (label) {
        "Gol" -> draft.gol
        "Asistencia" -> draft.asistencia
        "Amarilla" -> draft.amarilla
        "Roja" -> draft.roja
        "Disparos al Arco" -> draft.disparosAlArco
        "Ocasiones de Gol" -> draft.ocasionesDeGol
        "Pelotas Perdidas" -> draft.pelotasPerdidas
        "Pelotas Recuperadas" -> draft.pelotasRecuperadas
        "Centros Buenos" -> draft.centrosBuenos
        "Centros Malos" -> draft.centrosMalos
        else -> 0
    }
}

fun dualFavorValue(draft: PlayerStatsDraft, label: String): Int {
    return when (label) {
        "Falta a Favor" -> draft.faltaAFavor
        "Corner a Favor" -> draft.cornerAFavor
        "Tiro Libre a Favor" -> draft.tiroLibreAFavor
        "Tiro Libre Lateral a Favor" -> draft.tiroLibreLateralAFavor
        else -> 0
    }
}

fun dualContraValue(draft: PlayerStatsDraft, label: String): Int {
    return when (label) {
        "Falta en Contra" -> draft.faltaEnContra
        "Corner en Contra" -> draft.cornerEnContra
        "Tiro Libre en Contra" -> draft.tiroLibreEnContra
        "Tiro Libre Lateral en Contra" -> draft.tiroLibreLateralEnContra
        else -> 0
    }
}

fun increaseSingleStat(draft: PlayerStatsDraft, label: String): PlayerStatsDraft {
    return when (label) {
        "Gol" -> draft.copy(gol = draft.gol + 1)
        "Asistencia" -> draft.copy(asistencia = draft.asistencia + 1)
        "Amarilla" -> draft.copy(amarilla = draft.amarilla + 1)
        "Roja" -> draft.copy(roja = draft.roja + 1)
        "Disparos al Arco" -> draft.copy(disparosAlArco = draft.disparosAlArco + 1)
        "Ocasiones de Gol" -> draft.copy(ocasionesDeGol = draft.ocasionesDeGol + 1)
        "Pelotas Perdidas" -> draft.copy(pelotasPerdidas = draft.pelotasPerdidas + 1)
        "Pelotas Recuperadas" -> draft.copy(pelotasRecuperadas = draft.pelotasRecuperadas + 1)
        "Centros Buenos" -> draft.copy(centrosBuenos = draft.centrosBuenos + 1)
        "Centros Malos" -> draft.copy(centrosMalos = draft.centrosMalos + 1)
        else -> draft
    }
}

fun decreaseSingleStat(draft: PlayerStatsDraft, label: String): PlayerStatsDraft {
    return when (label) {
        "Gol" -> draft.copy(gol = (draft.gol - 1).coerceAtLeast(0))
        "Asistencia" -> draft.copy(asistencia = (draft.asistencia - 1).coerceAtLeast(0))
        "Amarilla" -> draft.copy(amarilla = (draft.amarilla - 1).coerceAtLeast(0))
        "Roja" -> draft.copy(roja = (draft.roja - 1).coerceAtLeast(0))
        "Disparos al Arco" -> draft.copy(disparosAlArco = (draft.disparosAlArco - 1).coerceAtLeast(0))
        "Ocasiones de Gol" -> draft.copy(ocasionesDeGol = (draft.ocasionesDeGol - 1).coerceAtLeast(0))
        "Pelotas Perdidas" -> draft.copy(pelotasPerdidas = (draft.pelotasPerdidas - 1).coerceAtLeast(0))
        "Pelotas Recuperadas" -> draft.copy(pelotasRecuperadas = (draft.pelotasRecuperadas - 1).coerceAtLeast(0))
        "Centros Buenos" -> draft.copy(centrosBuenos = (draft.centrosBuenos - 1).coerceAtLeast(0))
        "Centros Malos" -> draft.copy(centrosMalos = (draft.centrosMalos - 1).coerceAtLeast(0))
        else -> draft
    }
}

fun increaseDualFavorStat(draft: PlayerStatsDraft, label: String): PlayerStatsDraft {
    return when (label) {
        "Falta a Favor" -> draft.copy(faltaAFavor = draft.faltaAFavor + 1)
        "Corner a Favor" -> draft.copy(cornerAFavor = draft.cornerAFavor + 1)
        "Tiro Libre a Favor" -> draft.copy(tiroLibreAFavor = draft.tiroLibreAFavor + 1)
        "Tiro Libre Lateral a Favor" -> draft.copy(tiroLibreLateralAFavor = draft.tiroLibreLateralAFavor + 1)
        else -> draft
    }
}

fun decreaseDualFavorStat(draft: PlayerStatsDraft, label: String): PlayerStatsDraft {
    return when (label) {
        "Falta a Favor" -> draft.copy(faltaAFavor = (draft.faltaAFavor - 1).coerceAtLeast(0))
        "Corner a Favor" -> draft.copy(cornerAFavor = (draft.cornerAFavor - 1).coerceAtLeast(0))
        "Tiro Libre a Favor" -> draft.copy(tiroLibreAFavor = (draft.tiroLibreAFavor - 1).coerceAtLeast(0))
        "Tiro Libre Lateral a Favor" -> draft.copy(tiroLibreLateralAFavor = (draft.tiroLibreLateralAFavor - 1).coerceAtLeast(0))
        else -> draft
    }
}

fun increaseDualContraStat(draft: PlayerStatsDraft, label: String): PlayerStatsDraft {
    return when (label) {
        "Falta en Contra" -> draft.copy(faltaEnContra = draft.faltaEnContra + 1)
        "Corner en Contra" -> draft.copy(cornerEnContra = draft.cornerEnContra + 1)
        "Tiro Libre en Contra" -> draft.copy(tiroLibreEnContra = draft.tiroLibreEnContra + 1)
        "Tiro Libre Lateral en Contra" -> draft.copy(tiroLibreLateralEnContra = draft.tiroLibreLateralEnContra + 1)
        else -> draft
    }
}

fun decreaseDualContraStat(draft: PlayerStatsDraft, label: String): PlayerStatsDraft {
    return when (label) {
        "Falta en Contra" -> draft.copy(faltaEnContra = (draft.faltaEnContra - 1).coerceAtLeast(0))
        "Corner en Contra" -> draft.copy(cornerEnContra = (draft.cornerEnContra - 1).coerceAtLeast(0))
        "Tiro Libre en Contra" -> draft.copy(tiroLibreEnContra = (draft.tiroLibreEnContra - 1).coerceAtLeast(0))
        "Tiro Libre Lateral en Contra" -> draft.copy(tiroLibreLateralEnContra = (draft.tiroLibreLateralEnContra - 1).coerceAtLeast(0))
        else -> draft
    }
}

@Composable
fun SingleStatCard(
    stat: SingleStatUi,
    value: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = stat.icon,
                contentDescription = stat.label,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stat.label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onIncrease,
                    modifier = Modifier.size(width = 52.dp, height = 40.dp)
                ) {
                    Text("+")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onDecrease,
                    modifier = Modifier.size(width = 52.dp, height = 40.dp)
                ) {
                    Text("-")
                }
            }
        }
    }
}

@Composable
fun DualStatCard(
    stat: DualStatUi,
    favorValue: Int,
    contraValue: Int,
    onFavorIncrease: () -> Unit,
    onFavorDecrease: () -> Unit,
    onContraIncrease: () -> Unit,
    onContraDecrease: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stat.label,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = stat.favorIcon,
                        contentDescription = stat.favorLabel,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stat.favorLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = favorValue.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row {
                        Button(
                            onClick = onFavorIncrease,
                            modifier = Modifier.size(width = 42.dp, height = 36.dp)
                        ) {
                            Text("+")
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedButton(
                            onClick = onFavorDecrease,
                            modifier = Modifier.size(width = 42.dp, height = 36.dp)
                        ) {
                            Text("-")
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = stat.contraIcon,
                        contentDescription = stat.contraLabel,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = stat.contraLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = contraValue.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row {
                        Button(
                            onClick = onContraIncrease,
                            modifier = Modifier.size(width = 42.dp, height = 36.dp)
                        ) {
                            Text("+")
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedButton(
                            onClick = onContraDecrease,
                            modifier = Modifier.size(width = 42.dp, height = 36.dp)
                        ) {
                            Text("-")
                        }
                    }
                }
            }
        }
    }
}