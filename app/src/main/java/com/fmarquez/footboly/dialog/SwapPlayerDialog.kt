package com.fmarquez.footboly.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fmarquez.footboly.modelos.Player

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwapPlayerDialog(
    starters: List<Player>,
    substitutes: List<Player>,
    starterSelected: Player?,
    subSelected: Player?,
    swapMinute: String,
    onStarterSelected: (Player) -> Unit,
    onSubSelected: (Player) -> Unit,
    onMinuteChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isMinuteValid = swapMinute.toIntOrNull() != null
    val canConfirm = starterSelected != null && subSelected != null && isMinuteValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar cambio") },
        text = {
            Column {
                Text("Selecciona un titular y un reserva para intercambiar")
                Spacer(modifier = Modifier.height(12.dp))

                Text("Titular que sale")
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    starters.forEach { player ->
                        AssistChip(
                            onClick = { onStarterSelected(player) },
                            label = {
                                Text(
                                    if (starterSelected?.id == player.id) {
                                        "✓ ${player.name}"
                                    } else {
                                        player.name
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Reserva que entra")
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    substitutes.forEach { player ->
                        AssistChip(
                            onClick = { onSubSelected(player) },
                            label = {
                                Text(
                                    if (subSelected?.id == player.id) {
                                        "✓ ${player.name}"
                                    } else {
                                        player.name
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = swapMinute,
                    onValueChange = { value ->
                        onMinuteChange(value.filter { it.isDigit() })
                    },
                    label = { Text("Minuto del cambio") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = canConfirm
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}