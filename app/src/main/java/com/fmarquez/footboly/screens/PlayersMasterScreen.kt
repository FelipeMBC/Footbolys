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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fmarquez.footboly.dialog.AddPlayerDialog
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.navigation.Screen
import com.fmarquez.footboly.util.hexToColor
import com.fmarquez.footboly.util.teamColorLight
import com.fmarquez.footboly.vm.FutbolViewModel

private val BgColor      = Color(0xFFF7F7F5)
private val SurfaceColor = Color(0xFFFFFFFF)
private val TextPrimary  = Color(0xFF111111)
private val TextSecondary = Color(0xFF888888)
private val BorderColor  = Color(0xFFE0E0DC)
private val ErrorRed     = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersMasterScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val team = vm.selectedTeam ?: return
    val currentMatch = vm.currentMatch
    val context = LocalContext.current

    // Colores dinámicos del equipo
    val teamColor      = hexToColor(team.shirtColorHex)
    val teamColorLight = teamColorLight(teamColor)

    var showAddDialog by remember { mutableStateOf(false) }
    var playerName by remember { mutableStateOf("") }
    var playerNumber by remember { mutableStateOf("") }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMinPlayersWarning by remember { mutableStateOf(false) }
    val matchInCourse = currentMatch?.isStarted == true && currentMatch.isFinished == false

    LaunchedEffect(team.players.size) {
        showMinPlayersWarning = team.players.size < 5
    }

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (team.logoUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(team.logoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Text(text = team.logoEmoji, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = team.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
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
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Jugadores",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(teamColorLight)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${team.players.size}/30",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = teamColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (team.players.size < 30) showAddDialog = true
                        else Toast.makeText(context, "Máximo 30 jugadores por equipo", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = teamColor,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Agregar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        when {
                            matchInCourse -> {
                                Toast.makeText(
                                    context,
                                    "Partido en curso. Opción: Ver",
                                    Toast.LENGTH_SHORT
                                ).show()

                                navHostController.navigate(Screen.MATCH_LIVE.route) {
                                    launchSingleTop = true
                                }
                            }

                            team.players.size < 5 -> showMinPlayersWarning = true

                            else -> vm.createNewMatch {
                                navHostController.navigate(Screen.MATCH_CONFIG.route)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextPrimary,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("Nuevo partido", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { navHostController.navigate(Screen.MATCH_TIMELINE.route) },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceColor,
                        contentColor = TextPrimary
                    )
                ) {
                    Text("Ver partidos", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Opción no disponible",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    enabled = false,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceColor,
                        contentColor = TextPrimary,
                        disabledContainerColor = Color(0xFFF3F3F3),
                        disabledContentColor = Color(0xFF9A9A9A)
                    )
                ) {
                    Text("Reporte", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
            ) {
                if (team.players.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚽", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Sin jugadores aún", color = TextSecondary, fontSize = 15.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp)
                    ) {
                        items(team.players.sortedBy { it.number }, key = { it.id }) { player ->
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
                                        .background(teamColorLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = player.number.toString(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = teamColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(player.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                                    Text("Camiseta N° ${player.number}", fontSize = 12.sp, color = TextSecondary)
                                }

                                IconButton(
                                    onClick = {
                                        playerToDelete = player
                                        showDeleteDialog = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPlayerDialog(
            playerName = playerName,
            playerNumber = playerNumber,
            onNameChange = { playerName = it },
            onNumberChange = { playerNumber = it },
            onDismiss = {
                showAddDialog = false
                playerName = ""
                playerNumber = ""
                if (team.players.size < 5) showMinPlayersWarning = true
            },
            onConfirm = {
                val parsedNumber = playerNumber.toIntOrNull()
                when {
                    team.players.size >= 30 -> {
                        Toast.makeText(context, "Máximo 30 jugadores por equipo", Toast.LENGTH_SHORT).show()
                        showAddDialog = false
                    }
                    playerName.isBlank() -> {
                        Toast.makeText(context, "Ingresa el nombre del jugador", Toast.LENGTH_SHORT).show()
                    }
                    parsedNumber == null || parsedNumber <= 0 -> {
                        Toast.makeText(context, "Ingresa un número de camiseta válido", Toast.LENGTH_SHORT).show()
                    }
                    team.players.any { it.number == parsedNumber } -> {
                        Toast.makeText(context, "Ese número de camiseta ya está en uso", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        vm.addPlayer(playerName, parsedNumber)
                        playerName = ""
                        playerNumber = ""
                        showAddDialog = false
                        showMinPlayersWarning = team.players.size < 5
                    }
                }
            }
        )
    }

    if (showDeleteDialog && playerToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; playerToDelete = null },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Eliminar jugador", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("¿Eliminar a ${playerToDelete?.name}?", color = TextSecondary) },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; playerToDelete = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playerToDelete?.let {
                            vm.removePlayer(it)
                            Toast.makeText(context, "Jugador eliminado", Toast.LENGTH_SHORT).show()
                        }
                        showDeleteDialog = false
                        playerToDelete = null
                        if ((vm.selectedTeam?.players?.size ?: 0) < 5) showMinPlayersWarning = true
                    }
                ) {
                    Text("Eliminar", color = ErrorRed, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    if (showMinPlayersWarning) {
        AlertDialog(
            onDismissRequest = { showMinPlayersWarning = false; showAddDialog = true },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Equipo incompleto", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Hay menos de 5 jugadores. Agrega más para poder iniciar partidos.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showMinPlayersWarning = false; showAddDialog = true }) {
                    Text("Agregar jugador", color = teamColor, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMinPlayersWarning = false }) {
                    Text("Cerrar", color = TextSecondary)
                }
            }
        )
    }
}