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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
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
    val currentMatch = vm.currentMatch

    val singleStats = remember {
        listOf(
            SingleStatUi("Gol", Icons.Default.Star),
            SingleStatUi("Asistencia", Icons.Default.Send),
            SingleStatUi("Amarilla", Icons.Default.Warning),
            SingleStatUi("Roja", Icons.Default.Star),
            SingleStatUi("Disparos al Arco", Icons.Default.Star),
            SingleStatUi("Ocasiones de Gol", Icons.Default.Star),
            SingleStatUi("Pelotas Perdidas", Icons.Default.Clear),
            SingleStatUi("Pelotas Recuperadas", Icons.Default.Star),
            SingleStatUi("Centros Buenos", Icons.Default.Send),
            SingleStatUi("Centros Malos", Icons.Default.Star)
        )
    }

    val dualStats = remember {
        listOf(
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
    }

    val singleValues = remember {
        singleStats.associate { it.label to mutableIntStateOf(it.initialValue) }
    }

    val dualFavorValues = remember {
        dualStats.associate { it.favorLabel to mutableIntStateOf(it.favorInitial) }
    }

    val dualContraValues = remember {
        dualStats.associate { it.contraLabel to mutableIntStateOf(it.contraInitial) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val timestamp = vm.getElapsedMatchTimeLabel()
                            val savedLines = mutableListOf<String>()

                            singleStats.forEach { stat ->
                                val value = singleValues[stat.label]?.intValue ?: 0
                                if (value > 0) {
                                    vm.addStatEvent(
                                        playerName = player.name,
                                        type = stat.label,
                                        count = value
                                    )
                                    savedLines.add("${stat.label}: $value $timestamp")
                                }
                            }

                            dualStats.forEach { stat ->
                                val favorValue = dualFavorValues[stat.favorLabel]?.intValue ?: 0
                                val contraValue = dualContraValues[stat.contraLabel]?.intValue ?: 0

                                if (favorValue > 0) {
                                    vm.addStatEvent(
                                        playerName = player.name,
                                        type = stat.favorLabel,
                                        count = favorValue
                                    )
                                    savedLines.add("${stat.favorLabel}: $favorValue $timestamp")
                                }

                                if (contraValue > 0) {
                                    vm.addStatEvent(
                                        playerName = player.name,
                                        type = stat.contraLabel,
                                        count = contraValue
                                    )
                                    savedLines.add("${stat.contraLabel}: $contraValue $timestamp")
                                }
                            }

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
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
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
                        text = "ID Partido: ${currentMatch?.id ?: 0}",
                        fontSize = 15.sp
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
                    SingleStatCard(
                        stat = stat,
                        value = singleValues[stat.label]?.intValue ?: 0,
                        onIncrease = {
                            val state = singleValues[stat.label]
                            if (state != null) state.intValue++
                        },
                        onDecrease = {
                            val state = singleValues[stat.label]
                            if (state != null && state.intValue > 0) state.intValue--
                        }
                    )
                }

                items(dualStats) { stat ->
                    DualStatCard(
                        stat = stat,
                        favorValue = dualFavorValues[stat.favorLabel]?.intValue ?: 0,
                        contraValue = dualContraValues[stat.contraLabel]?.intValue ?: 0,
                        onFavorIncrease = {
                            val state = dualFavorValues[stat.favorLabel]
                            if (state != null) state.intValue++
                        },
                        onFavorDecrease = {
                            val state = dualFavorValues[stat.favorLabel]
                            if (state != null && state.intValue > 0) state.intValue--
                        },
                        onContraIncrease = {
                            val state = dualContraValues[stat.contraLabel]
                            if (state != null) state.intValue++
                        },
                        onContraDecrease = {
                            val state = dualContraValues[stat.contraLabel]
                            if (state != null && state.intValue > 0) state.intValue--
                        }
                    )
                }
            }
        }
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