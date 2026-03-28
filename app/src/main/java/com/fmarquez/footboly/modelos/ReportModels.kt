package com.fmarquez.footboly.modelos

enum class ReportMetricKind {
    EVENT_COUNT,
    MINUTES,
    STARTS
}

data class ReportMetricDefinition(
    val key: String,
    val label: String,
    val kind: ReportMetricKind = ReportMetricKind.EVENT_COUNT,
    val supportsTimeline: Boolean = true,
    val supportsPlayerBreakdown: Boolean = true
)

data class ReportRankingRow(
    val position: Int,
    val playerId: Int,
    val playerName: String,
    val shirtNumber: Int,
    val total: Double,
    val average: Double,
    val matchesPlayed: Int
)

data class ReportTimeBlock(
    val label: String,
    val total: Int
)

data class ReportTimeBreakdown(
    val metric: ReportMetricDefinition,
    val matchLabel: String,
    val total: Int,
    val blocks: List<ReportTimeBlock>
)

data class ReportPlayerStatRow(
    val metric: ReportMetricDefinition,
    val total: Double,
    val average: Double,
    val position: Int?
)

data class ReportPlayerSummary(
    val playerId: Int,
    val playerName: String,
    val shirtNumber: Int,
    val matchesPlayed: Int,
    val teamMatches: Int,
    val totalMinutes: Double,
    val averageMinutes: Double,
    val starts: Int,
    val stats: List<ReportPlayerStatRow>
)
