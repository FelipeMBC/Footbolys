package com.fmarquez.footboly.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.vm.FutbolViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchTimelineScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val match = vm.currentMatch

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ver partido") },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (match == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aún no hay partido registrado")
            }
        } else {
            val orderedEvents = match.events.sortedBy { it.minute }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Línea de tiempo - ${match.teamName}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (orderedEvents.isEmpty()) {
                    item {
                        Text("No hay eventos registrados todavía")
                    }
                } else {
                    items(orderedEvents) { event ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Minuto ${event.minute}", fontWeight = FontWeight.Bold)
                                Text("Evento: ${event.type}")
                                Text("Jugador: ${event.playerName}")
                                if (event.detail.isNotBlank()) {
                                    Text("Detalle: ${event.detail}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}