package com.fmarquez.footboly.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fmarquez.footboly.modelos.MatchRecord
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.modelos.ReportMetricDefinition
import com.fmarquez.footboly.modelos.ReportMetricKind
import com.fmarquez.footboly.modelos.ReportPlayerStatRow
import com.fmarquez.footboly.modelos.ReportPlayerSummary
import com.fmarquez.footboly.modelos.ReportRankingRow
import com.fmarquez.footboly.modelos.ReportTimeBreakdown
import com.fmarquez.footboly.vm.FutbolViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val ReportsBg = Color(0xFFF4F4F2)
private val ReportsSurface = Color(0xFFFFFFFF)
private val ReportsText = Color(0xFF111111)
private val ReportsTextSecondary = Color(0xFF666666)
private val ReportsBlue = Color(0xFF125F84)
private val ReportsBlueDark = Color(0xFF0D3B53)
private val ReportsGreen = Color(0xFF58B12C)
private val ReportsGreenLight = Color(0xFFF5FBF0)
private val ReportsBorder = Color(0xFFE3E3DF)
private val ReportsMuted = Color(0xFFF2F2F0)

private enum class ReportSection(val title: String) {
    RANKING("Ranking"),
    METRICS_BY_TIME("Métricas por Tiempo"),
    STATS("Estadísticas")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReporteScreen(
    vm: FutbolViewModel,
    navHostController: NavHostController
) {
    val selectedTeam = vm.selectedTeam
    val rankingMetrics = vm.getReportRankingMetrics()
    val timelineMetrics = vm.getReportTimelineMetrics()
    val finishedMatches = vm.getReportMatchesForSelectedTeam()
    val players = vm.getReportPlayers()

    var selectedSection by rememberSaveable { mutableStateOf(ReportSection.RANKING) }
    var selectedRankingMetricKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTimelineMetricKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTimelineMatchId by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedPlayerId by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(rankingMetrics) {
        val current = selectedRankingMetricKey
        if (current == null || rankingMetrics.none { it.key == current }) {
            selectedRankingMetricKey = rankingMetrics.firstOrNull()?.key
        }
    }

    LaunchedEffect(timelineMetrics) {
        val current = selectedTimelineMetricKey
        if (current == null || timelineMetrics.none { it.key == current }) {
            selectedTimelineMetricKey = timelineMetrics.firstOrNull()?.key
        }
    }

    LaunchedEffect(players) {
        val current = selectedPlayerId
        if (current == null || players.none { it.id == current }) {
            selectedPlayerId = players.firstOrNull()?.id
        }
    }

    val selectedRankingMetric = rankingMetrics.firstOrNull { it.key == selectedRankingMetricKey }
    val selectedTimelineMetric = timelineMetrics.firstOrNull { it.key == selectedTimelineMetricKey }
    val rankingRows = selectedRankingMetricKey?.let(vm::buildReportRanking).orEmpty()
    val timeBreakdown = selectedTimelineMetricKey?.let { vm.buildReportTimeBreakdown(it, selectedTimelineMatchId) }
    val playerSummary = selectedPlayerId?.let(vm::buildPlayerReport)

    val isLoading = selectedTeam == null && vm.teams.isEmpty()
    val hasHistory = finishedMatches.isNotEmpty()

    Scaffold(
        containerColor = ReportsBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Reportes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = ReportsText
                        )
                        selectedTeam?.let {
                            Text(
                                text = it.name,
                                fontSize = 12.sp,
                                color = ReportsTextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navHostController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = ReportsText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ReportsBg)
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                ReportCenteredState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    title = "Cargando datos",
                    subtitle = "Esperando equipos y partidos guardados.",
                    loading = true
                )
            }

            selectedTeam == null -> {
                ReportCenteredState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    title = "Sin equipo seleccionado",
                    subtitle = "Selecciona un equipo para ver sus reportes históricos."
                )
            }

            !hasHistory -> {
                ReportCenteredState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    title = "Sin historial disponible",
                    subtitle = "Este equipo todavía no tiene partidos finalizados guardados."
                )
            }

            else -> {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    val compact = maxWidth < 980.dp

                    if (compact) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ReportsLeftMenu(
                                selectedSection = selectedSection,
                                onSectionSelected = { selectedSection = it },
                                modifier = Modifier.fillMaxWidth()
                            )

                            ReportsMainContent(
                                section = selectedSection,
                                rankingMetric = selectedRankingMetric,
                                rankingRows = rankingRows,
                                timeBreakdown = timeBreakdown,
                                playerSummary = playerSummary,
                                modifier = Modifier.fillMaxWidth()
                            )

                            ReportsSidePanel(
                                section = selectedSection,
                                rankingMetrics = rankingMetrics,
                                selectedRankingMetricKey = selectedRankingMetricKey,
                                onRankingMetricSelected = { selectedRankingMetricKey = it },
                                timelineMetrics = timelineMetrics,
                                selectedTimelineMetric = selectedTimelineMetric,
                                onTimelineMetricSelected = { selectedTimelineMetricKey = it },
                                matches = finishedMatches,
                                selectedTimelineMatchId = selectedTimelineMatchId,
                                onTimelineMatchSelected = { selectedTimelineMatchId = it },
                                players = players,
                                selectedPlayerId = selectedPlayerId,
                                onPlayerSelected = { selectedPlayerId = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            ReportsLeftMenu(
                                selectedSection = selectedSection,
                                onSectionSelected = { selectedSection = it },
                                modifier = Modifier.weight(0.95f)
                            )

                            ReportsMainContent(
                                section = selectedSection,
                                rankingMetric = selectedRankingMetric,
                                rankingRows = rankingRows,
                                timeBreakdown = timeBreakdown,
                                playerSummary = playerSummary,
                                modifier = Modifier.weight(1.8f)
                            )

                            ReportsSidePanel(
                                section = selectedSection,
                                rankingMetrics = rankingMetrics,
                                selectedRankingMetricKey = selectedRankingMetricKey,
                                onRankingMetricSelected = { selectedRankingMetricKey = it },
                                timelineMetrics = timelineMetrics,
                                selectedTimelineMetric = selectedTimelineMetric,
                                onTimelineMetricSelected = { selectedTimelineMetricKey = it },
                                matches = finishedMatches,
                                selectedTimelineMatchId = selectedTimelineMatchId,
                                onTimelineMatchSelected = { selectedTimelineMatchId = it },
                                players = players,
                                selectedPlayerId = selectedPlayerId,
                                onPlayerSelected = { selectedPlayerId = it },
                                modifier = Modifier.weight(1.0f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsMainContent(
    section: ReportSection,
    rankingMetric: ReportMetricDefinition?,
    rankingRows: List<ReportRankingRow>,
    timeBreakdown: ReportTimeBreakdown?,
    playerSummary: ReportPlayerSummary?,
    modifier: Modifier = Modifier
) {
    when (section) {
        ReportSection.RANKING -> RankingContent(
            metric = rankingMetric,
            rows = rankingRows,
            modifier = modifier
        )

        ReportSection.METRICS_BY_TIME -> MetricsByTimeContent(
            breakdown = timeBreakdown,
            modifier = modifier
        )

        ReportSection.STATS -> PlayerStatsReportContent(
            summary = playerSummary,
            modifier = modifier
        )
    }
}

@Composable
private fun ReportsSidePanel(
    section: ReportSection,
    rankingMetrics: List<ReportMetricDefinition>,
    selectedRankingMetricKey: String?,
    onRankingMetricSelected: (String) -> Unit,
    timelineMetrics: List<ReportMetricDefinition>,
    selectedTimelineMetric: ReportMetricDefinition?,
    onTimelineMetricSelected: (String) -> Unit,
    matches: List<MatchRecord>,
    selectedTimelineMatchId: Int?,
    onTimelineMatchSelected: (Int?) -> Unit,
    players: List<Player>,
    selectedPlayerId: Int?,
    onPlayerSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ReportsRightPanel(modifier = modifier) {
        when (section) {
            ReportSection.RANKING -> {
                Text(
                    text = "Categorías",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ReportsSurface
                )

                rankingMetrics.forEach { metric ->
                    ReportsActionButton(
                        text = metric.label,
                        selected = metric.key == selectedRankingMetricKey,
                        onClick = { onRankingMetricSelected(metric.key) }
                    )
                }
            }

            ReportSection.METRICS_BY_TIME -> {
                Text(
                    text = "Filtros",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ReportsSurface
                )

                ReportsDropdownButton(
                    label = selectedTimelineMetric?.label ?: "Métrica",
                    options = timelineMetrics.map { it.label to it.key },
                    onOptionSelected = onTimelineMetricSelected
                )

                ReportsDropdownButton(
                    label = selectedTimelineMatchId?.let { id ->
                        matches.firstOrNull { it.id == id }?.let(::reportMatchLabel)
                    } ?: "Todos los partidos",
                    options = buildList {
                        add("Todos los partidos" to "ALL")
                        addAll(matches.map { reportMatchLabel(it) to it.id.toString() })
                    },
                    onOptionSelected = { value ->
                        onTimelineMatchSelected(value.takeUnless { it == "ALL" }?.toIntOrNull())
                    }
                )
            }

            ReportSection.STATS -> {
                Text(
                    text = "Jugadores",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ReportsSurface
                )

                players.forEach { player ->
                    ReportsActionButton(
                        text = "#${player.number} ${player.name}",
                        selected = player.id == selectedPlayerId,
                        onClick = { onPlayerSelected(player.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportsLeftMenu(
    selectedSection: ReportSection,
    onSectionSelected: (ReportSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = ReportsBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ReportSection.entries.forEach { section ->
                ReportsMenuButton(
                    text = section.title,
                    selected = selectedSection == section,
                    onClick = { onSectionSelected(section) }
                )
            }
        }
    }
}

@Composable
private fun ReportsRightPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = ReportsBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun RankingContent(
    metric: ReportMetricDefinition?,
    rows: List<ReportRankingRow>,
    modifier: Modifier = Modifier
) {
    ReportCardShell(
        title = metric?.label ?: "Ranking",
        subtitle = "Totales y promedios históricos por jugador.",
        modifier = modifier
    ) {
        if (metric == null) {
            ReportInnerEmpty("No hay categorías disponibles.")
            return@ReportCardShell
        }

        if (rows.isEmpty()) {
            ReportInnerEmpty("No hay registros históricos para esta categoría.")
            return@ReportCardShell
        }

        ReportsTableHeader(
            columns = listOf(
                TableColumn("Pos", 0.14f),
                TableColumn("Jugador", 0.42f),
                TableColumn("Total", 0.16f, TextAlign.End),
                TableColumn("Prom", 0.16f, TextAlign.End),
                TableColumn("PJ", 0.12f, TextAlign.End)
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rows, key = { "${metric.key}_${it.playerId}" }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReportsSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, ReportsBorder, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TableCell("${row.position}°", 0.14f, fontWeight = FontWeight.Bold)
                    TableCell("#${row.shirtNumber} ${row.playerName}", 0.42f)
                    TableCell(formatMetricValue(metric, row.total), 0.16f, textAlign = TextAlign.End)
                    TableCell(formatAverageValue(metric, row.average), 0.16f, textAlign = TextAlign.End)
                    TableCell(row.matchesPlayed.toString(), 0.12f, textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun MetricsByTimeContent(
    breakdown: ReportTimeBreakdown?,
    modifier: Modifier = Modifier
) {
    ReportCardShell(
        title = breakdown?.metric?.label ?: "Métricas por Tiempo",
        subtitle = breakdown?.matchLabel ?: "Selecciona una métrica temporal.",
        modifier = modifier
    ) {
        if (breakdown == null) {
            ReportInnerEmpty("No hay información temporal disponible para la selección actual.")
            return@ReportCardShell
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            breakdown.blocks.forEach { block ->
                MetricTimeBlockCard(
                    label = block.label,
                    total = block.total
                )
            }

            MetricTimeBlockCard(
                label = "Total General",
                total = breakdown.total,
                highlighted = true
            )
        }
    }
}

@Composable
private fun PlayerStatsReportContent(
    summary: ReportPlayerSummary?,
    modifier: Modifier = Modifier
) {
    ReportCardShell(
        title = summary?.let { "#${it.shirtNumber} ${it.playerName}" } ?: "Estadísticas",
        subtitle = "Resumen histórico individual.",
        modifier = modifier
    ) {
        if (summary == null) {
            ReportInnerEmpty("Selecciona un jugador con historial disponible.")
            return@ReportCardShell
        }

        PlayerSummaryHeader(summary = summary)

        Spacer(modifier = Modifier.height(16.dp))

        ReportsTableHeader(
            columns = listOf(
                TableColumn("Categoría", 0.46f),
                TableColumn("Total", 0.18f, TextAlign.End),
                TableColumn("Prom", 0.18f, TextAlign.End),
                TableColumn("Pos", 0.18f, TextAlign.End)
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(summary.stats, key = { it.metric.key }) { row ->
                PlayerMetricRow(row = row)
            }
        }
    }
}

@Composable
private fun PlayerSummaryHeader(summary: ReportPlayerSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReportsGreenLight, RoundedCornerShape(18.dp))
            .border(1.dp, ReportsGreen.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryChip(
                label = "Partidos asistidos",
                value = "${summary.matchesPlayed}/${summary.teamMatches}",
                modifier = Modifier.weight(1f)
            )
            SummaryChip(
                label = "Titularidades",
                value = summary.starts.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryChip(
                label = "Minutos jugados",
                value = formatMinutes(summary.totalMinutes),
                modifier = Modifier.weight(1f)
            )
            SummaryChip(
                label = "Promedio minutos",
                value = formatMinutes(summary.averageMinutes),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(ReportsSurface, RoundedCornerShape(14.dp))
            .border(1.dp, ReportsBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = ReportsTextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ReportsText
        )
    }
}

@Composable
private fun PlayerMetricRow(row: ReportPlayerStatRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ReportsSurface, RoundedCornerShape(16.dp))
            .border(1.dp, ReportsBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(row.metric.label, 0.46f)
        TableCell(formatMetricValue(row.metric, row.total), 0.18f, textAlign = TextAlign.End)
        TableCell(formatAverageValue(row.metric, row.average), 0.18f, textAlign = TextAlign.End)
        TableCell(row.position?.let { "${it}°" } ?: "-", 0.18f, textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MetricTimeBlockCard(
    label: String,
    total: Int,
    highlighted: Boolean = false
) {
    val container = if (highlighted) ReportsGreenLight else ReportsSurface
    val border = if (highlighted) ReportsGreen.copy(alpha = 0.45f) else ReportsBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(18.dp))
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            color = ReportsText
        )

        Box(
            modifier = Modifier
                .background(ReportsBlueDark, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = total.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ReportCardShell(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(34.dp),
        colors = CardDefaults.cardColors(containerColor = ReportsBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, ReportsGreen, RoundedCornerShape(34.dp))
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ReportsText
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = ReportsTextSecondary
            )
            HorizontalDivider(color = ReportsBorder)
            content()
        }
    }
}

@Composable
private fun ReportsMenuButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) ReportsMuted else ReportsSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(background, RoundedCornerShape(18.dp))
            .border(2.dp, ReportsGreen, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            color = ReportsText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReportsActionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) ReportsGreenLight else ReportsSurface
    val borderColor = if (selected) ReportsGreen else ReportsGreen.copy(alpha = 0.95f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .background(background, RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            color = ReportsText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReportsDropdownButton(
    label: String,
    options: List<Pair<String, String>>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ReportsSurface, RoundedCornerShape(14.dp))
                .border(2.dp, ReportsGreen, RoundedCornerShape(14.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = ReportsText,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = ReportsBlueDark
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(ReportsSurface)
        ) {
            options.forEach { (title, value) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = title,
                            color = ReportsText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        expanded = false
                        onOptionSelected(value)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReportCenteredState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(color = ReportsBlue)
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(ReportsGreenLight, CircleShape)
                        .border(1.dp, ReportsGreen.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📊", fontSize = 24.sp)
                }
            }

            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = ReportsText,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = ReportsTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReportInnerEmpty(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = ReportsTextSecondary,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}

private data class TableColumn(
    val title: String,
    val weight: Float,
    val textAlign: TextAlign = TextAlign.Start
)

@Composable
private fun ReportsTableHeader(columns: List<TableColumn>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        columns.forEach { column ->
            TableCell(
                text = column.title,
                weight = column.weight,
                textAlign = column.textAlign,
                color = ReportsTextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = ReportsText,
    fontWeight: FontWeight = FontWeight.Medium,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        color = color,
        fontWeight = fontWeight,
        fontSize = fontSize,
        textAlign = textAlign,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

private fun reportMatchLabel(match: MatchRecord): String {
    val rival = match.rivalName.ifBlank { "Sin rival" }
    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        .format(Date(match.finishedAtMillis ?: match.createdAtMillis))
    return "$rival · $date"
}

private fun formatMetricValue(metric: ReportMetricDefinition, value: Double): String {
    return when (metric.kind) {
        ReportMetricKind.MINUTES -> formatMinutes(value)
        ReportMetricKind.STARTS, ReportMetricKind.EVENT_COUNT -> formatWholeOrOneDecimal(value)
    }
}

private fun formatAverageValue(metric: ReportMetricDefinition, value: Double): String {
    return when (metric.kind) {
        ReportMetricKind.MINUTES -> formatMinutes(value)
        ReportMetricKind.STARTS, ReportMetricKind.EVENT_COUNT -> formatOneDecimal(value)
    }
}

private fun formatMinutes(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

private fun formatWholeOrOneDecimal(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.roundToInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }
}

private fun formatOneDecimal(value: Double): String {
    return String.format(Locale.getDefault(), "%.1f", value)
}
