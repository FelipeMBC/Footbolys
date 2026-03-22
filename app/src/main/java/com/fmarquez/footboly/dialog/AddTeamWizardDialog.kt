package com.fmarquez.footboly.dialog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import coil.compose.AsyncImage
import coil.request.ImageRequest

private val SurfaceColor     = Color(0xFFFFFFFF)
private val AccentGreen      = Color(0xFF1E6B45)
private val AccentGreenLight = Color(0xFFE8F2EC)
private val TextPrimary      = Color(0xFF111111)
private val TextSecondary    = Color(0xFF888888)
private val BorderColor      = Color(0xFFE0E0DC)

@Composable
private fun MinimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentGreen,
            unfocusedBorderColor = BorderColor,
            focusedLabelColor = AccentGreen,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = AccentGreen
        )
    )
}

@Composable
private fun TabSelector(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF0F0EE))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        tabs.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selectedIndex == index) SurfaceColor else Color.Transparent)
                    .border(
                        width = if (selectedIndex == index) 1.dp else 0.dp,
                        color = if (selectedIndex == index) BorderColor else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (selectedIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selectedIndex == index) TextPrimary else TextSecondary
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// AddTeamPlayersDialog
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun AddTeamPlayersDialog(
    currentPlayerName: String,
    players: List<String>,
    onPlayerNameChange: (String) -> Unit,
    onAddPlayer: () -> Unit,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Agregar jugadores", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Entre 5 y 30 jugadores.", fontSize = 13.sp, color = TextSecondary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = currentPlayerName,
                        onValueChange = onPlayerNameChange,
                        label = { Text("Nombre", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGreen,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = AccentGreen,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentGreen
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentGreen)
                            .clickable { onAddPlayer() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Light)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Jugadores agregados", fontSize = 12.sp, color = TextSecondary)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AccentGreenLight)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text("${players.size}/30", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentGreen)
                    }
                }

                if (players.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    ) {
                        itemsIndexed(players) { index, player ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(AccentGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(player, fontSize = 14.sp, color = TextPrimary)
                            }
                            if (index < players.lastIndex) {
                                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(BorderColor))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onContinue,
                enabled = players.size >= 5,
                colors = ButtonDefaults.textButtonColors(contentColor = AccentGreen, disabledContentColor = TextSecondary)
            ) {
                Text("Continuar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                Text("Cancelar")
            }
        }
    )
}

// ────────────────────────────────────────────────────────────────────────────
// AddTeamEmojiDialog  ← tabs Emoji / Foto
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun AddTeamEmojiDialog(
    emojiValue: String,
    onEmojiChange: (String) -> Unit,
    logoUri: String?,
    onLogoUriChange: (String?) -> Unit,
    onDismiss: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(if (logoUri != null) 1 else 0) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onLogoUriChange(uri?.toString())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Logo del equipo", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                TabSelector(
                    tabs = listOf("Emoji", "Foto"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it }
                )

                if (selectedTab == 0) {
                    // ── Tab Emoji ─────────────────────────────────────────────
                    Text("Escribe un emoji para identificar tu equipo.", fontSize = 13.sp, color = TextSecondary)

                    if (emojiValue.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentGreenLight)
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emojiValue, fontSize = 48.sp)
                        }
                    }

                    MinimalTextField(value = emojiValue, onValueChange = onEmojiChange, label = "Emoji del equipo")

                } else {
                    // ── Tab Foto ──────────────────────────────────────────────
                    Text("Elige una imagen desde tu galería.", fontSize = 13.sp, color = TextSecondary)

                    if (logoUri != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(logoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Logo del equipo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .clickable { galleryLauncher.launch("image/*") }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Cambiar", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentGreenLight)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🖼️", fontSize = 28.sp)
                                Text("Tocar para elegir foto", fontSize = 13.sp, color = AccentGreen, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onContinue, colors = ButtonDefaults.textButtonColors(contentColor = AccentGreen)) {
                Text("Continuar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onSkip, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                    Text("Omitir")
                }
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                    Text("Cancelar")
                }
            }
        }
    )
}

// ────────────────────────────────────────────────────────────────────────────
// AddTeamNameDialog  ← preview con foto o emoji
// ────────────────────────────────────────────────────────────────────────────
@Composable
fun AddTeamNameDialog(
    teamName: String,
    selectedEmoji: String,
    logoUri: String?,
    players: List<String>,
    onTeamNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreateTeam: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Nombre del equipo", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentGreenLight)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (logoUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(logoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.dp, BorderColor, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SurfaceColor)
                                .border(1.dp, BorderColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(selectedEmoji, fontSize = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (teamName.isBlank()) "Nombre del equipo" else teamName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = if (teamName.isBlank()) TextSecondary else TextPrimary
                        )
                        Text("${players.size} jugadores", fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.Medium)
                    }
                }

                MinimalTextField(value = teamName, onValueChange = onTeamNameChange, label = "Nombre del equipo")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCreateTeam,
                enabled = teamName.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(contentColor = AccentGreen, disabledContentColor = TextSecondary)
            ) {
                Text("Crear equipo", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) {
                Text("Cancelar")
            }
        }
    )
}