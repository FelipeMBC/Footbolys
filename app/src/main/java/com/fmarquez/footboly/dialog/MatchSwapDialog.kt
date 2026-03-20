package com.fmarquez.footboly.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fmarquez.footboly.modelos.Player

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
    val canConfirm = selectedSubstitute != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambio de jugador") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Titular que sale: ${starter.name}")
                Text("Selecciona una reserva que entrará:")

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    substitutes.forEach { player ->
                        AssistChip(
                            onClick = { onSelectSubstitute(player) },
                            label = {
                                Text(
                                    if (selectedSubstitute?.id == player.id) {
                                        "✓ ${player.name}"
                                    } else {
                                        player.name
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = canConfirm
            ) {
                Text("Confirmar cambio")
            }
        }
    )
}