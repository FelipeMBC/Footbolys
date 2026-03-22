package com.fmarquez.footboly.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fmarquez.footboly.modelos.Player

private val SurfaceColor     = Color(0xFFFFFFFF)
private val AccentGreen      = Color(0xFF1E6B45)
private val AccentGreenLight = Color(0xFFE8F2EC)
private val TextPrimary      = Color(0xFF111111)
private val TextSecondary    = Color(0xFF888888)
private val BorderColor      = Color(0xFFE0E0DC)
private val ErrorRed         = Color(0xFFD32F2F)
private val ErrorRedLight    = Color(0xFFFFF0F0)
private val ErrorRedBorder   = Color(0xFFFFD6D6)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchSwapDialog(
    starter: Player,
    substitutes: List<Player>,
    selectedSubstitute: Player?,
    onSelectSubstitute: (Player) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Cambio de jugador",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Jugador que sale
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(ErrorRedLight)
                        .border(1.dp, ErrorRedBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ErrorRedBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↑", fontSize = 14.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Sale", fontSize = 11.sp, color = ErrorRed, fontWeight = FontWeight.Medium)
                        Text(starter.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }

                // Selector de reserva
                Text("¿Quién entra?", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    substitutes.forEach { player ->
                        val isSelected = selectedSubstitute?.id == player.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) AccentGreen else AccentGreenLight)
                                .border(
                                    1.dp,
                                    if (isSelected) AccentGreen else BorderColor,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { onSelectSubstitute(player) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = if (isSelected) "✓  ${player.name}" else player.name,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("Cancelar")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = selectedSubstitute != null,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AccentGreen,
                    disabledContentColor = TextSecondary
                )
            ) {
                Text("Confirmar cambio", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}