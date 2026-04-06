package com.fmarquez.footboly.vm

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fmarquez.footboly.data.local.db.FootbolyDatabase
import com.fmarquez.footboly.data.repository.FootballRepository
import com.fmarquez.footboly.dialog.TempPlayerInput
import com.fmarquez.footboly.modelos.MatchEvent
import com.fmarquez.footboly.modelos.MatchPlayerTime
import com.fmarquez.footboly.modelos.MatchRecord
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.modelos.PlayerStats
import com.fmarquez.footboly.modelos.PlayerStatsDraft
import com.fmarquez.footboly.modelos.Team
import com.fmarquez.footboly.modelos.ReportMetricDefinition
import com.fmarquez.footboly.modelos.ReportMetricKind
import com.fmarquez.footboly.modelos.ReportPlayerStatRow
import com.fmarquez.footboly.modelos.ReportPlayerSummary
import com.fmarquez.footboly.modelos.ReportRankingRow
import com.fmarquez.footboly.modelos.ReportTimeBlock
import com.fmarquez.footboly.modelos.ReportTimeBreakdown
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import org.json.JSONArray
import org.json.JSONObject

class FutbolViewModel(application: Application) : AndroidViewModel(application) {

    private enum class MatchPauseState {
        NONE,
        MANUAL,
        FIRST_HALF
    }

    private val database = FootbolyDatabase.getDatabase(application)
    private val repository = FootballRepository(
        teamDao = database.teamDao(),
        matchDao = database.matchDao()
    )

    val teams = mutableStateListOf<Team>()

    private var selectedTeamId by mutableStateOf<Int?>(null)

    val selectedTeam: Team?
        get() = teams.firstOrNull { it.id == selectedTeamId }

    var editingFinishedMatch by mutableStateOf<MatchRecord?>(null)
        private set

    var currentMatch by mutableStateOf<MatchRecord?>(null)
        private set

    var shouldShowFinishedDialog by mutableStateOf(false)
        private set

    var selectedFinishedMatch by mutableStateOf<MatchRecord?>(null)
        private set

    var selectedPlayerId by mutableStateOf<Int?>(null)
        private set

    var shouldShowEditResultDialog by mutableStateOf(false)
        private set

    var lastEditChanges by mutableStateOf<List<String>>(emptyList())
        private set

    val finishedMatches = mutableStateListOf<MatchRecord>()

    private var matchTimerJob: Job? = null

    private var matchPauseState by mutableStateOf(MatchPauseState.NONE)
    private var firstHalfRegistered by mutableStateOf(false)
    private var secondHalfStarted by mutableStateOf(false)

    val isMatchPaused: Boolean
        get() = matchPauseState != MatchPauseState.NONE

    private val playerStatsDrafts = mutableStateMapOf<String, PlayerStatsDraft>()
    private val originalPlayerStatsDrafts = mutableStateMapOf<String, PlayerStatsDraft>()

    init {
        viewModelScope.launch {
            repository.seedIfNeeded()
        }

        viewModelScope.launch {
            repository.observeTeams().collect { loadedTeams ->
                val previousSelectedTeamId = selectedTeamId

                teams.clear()
                teams.addAll(loadedTeams)

                selectedTeamId = when {
                    loadedTeams.isEmpty() -> null
                    previousSelectedTeamId != null &&
                            loadedTeams.any { it.id == previousSelectedTeamId } -> previousSelectedTeamId
                    else -> loadedTeams.first().id
                }
            }
        }

        viewModelScope.launch {
            repository.observeLatestMatch().collect { loadedMatch ->
                currentMatch = loadedMatch

                if (loadedMatch != null && selectedTeamId == null) {
                    selectedTeamId = loadedMatch.teamId
                }

                if (selectedFinishedMatch?.id == loadedMatch?.id) {
                    selectedFinishedMatch = loadedMatch
                }

                if (editingFinishedMatch?.id == loadedMatch?.id) {
                    editingFinishedMatch = loadedMatch
                }

                syncPauseStateWithMatch(loadedMatch)
            }
        }

        viewModelScope.launch {
            repository.observeFinishedMatches().collect { loadedFinishedMatches ->
                finishedMatches.clear()
                finishedMatches.addAll(loadedFinishedMatches)

                selectedFinishedMatch = selectedFinishedMatch?.let { current ->
                    loadedFinishedMatches.firstOrNull { it.id == current.id }
                        ?: currentMatch?.takeIf { it.id == current.id && it.isFinished }
                }

                editingFinishedMatch = editingFinishedMatch?.let { current ->
                    loadedFinishedMatches.firstOrNull { it.id == current.id }
                        ?: currentMatch?.takeIf { it.id == current.id && it.isFinished }
                }
            }
        }
    }

    private fun copyImageToInternalStorage(uriString: String): String? {
        return try {
            val context = getApplication<Application>()
            val uri = Uri.parse(uriString)

            val logosDir = File(context.filesDir, "team_logos")
            if (!logosDir.exists()) logosDir.mkdirs()

            val fileName = "logo_${System.currentTimeMillis()}.jpg"
            val destFile = File(logosDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun statsDraftKey(matchId: Int, playerId: Int): String {
        return "${matchId}_$playerId"
    }

    private fun playerToJson(player: Player): JSONObject {
        return JSONObject().apply {
            put("id", player.id)
            put("name", player.name)
            put("number", player.number)
        }
    }

    private fun playersToJson(players: List<Player>): JSONArray {
        return JSONArray().apply {
            players.forEach { put(playerToJson(it)) }
        }
    }

    private fun teamPlayersToJson(team: Team?): JSONArray {
        return JSONArray().apply {
            team?.players
                ?.sortedBy { it.number }
                ?.forEach { player -> put(playerToJson(player)) }
        }
    }

    private fun matchEventToJson(event: MatchEvent): JSONObject {
        return JSONObject().apply {
            put("minute", event.minute)
            if (event.playerId == null) put("playerId", JSONObject.NULL) else put("playerId", event.playerId)
            put("type", event.type)
            put("playerName", event.playerName)
            put("detail", event.detail)
            put("timestampLabel", event.timestampLabel)
        }
    }

    private fun matchEventsToJson(events: List<MatchEvent>): JSONArray {
        return JSONArray().apply {
            events.forEach { put(matchEventToJson(it)) }
        }
    }

    private fun playerTimesToJson(playerTimes: Map<Int, MatchPlayerTime>): JSONArray {
        return JSONArray().apply {
            playerTimes.values
                .sortedBy { it.playerId }
                .forEach { playerTime ->
                    put(
                        JSONObject().apply {
                            put("playerId", playerTime.playerId)
                            put("accumulatedSeconds", playerTime.accumulatedSeconds)
                            put("isCurrentlyPlaying", playerTime.isCurrentlyPlaying)
                            if (playerTime.lastEntrySecond == null) {
                                put("lastEntrySecond", JSONObject.NULL)
                            } else {
                                put("lastEntrySecond", playerTime.lastEntrySecond)
                            }
                        }
                    )
                }
        }
    }

    fun exportMatchToJson(match: MatchRecord, includeFullTeam: Boolean = false): String {
        val teamSnapshot = teams.firstOrNull { it.id == match.teamId }
            ?: selectedTeam?.takeIf { it.id == match.teamId }

        val root = JSONObject().apply {
            put("schemaVersion", 2)
            put("exportedAtMillis", System.currentTimeMillis())
            put("transferType", if (includeFullTeam) "TEAM_AND_MATCH" else "MATCH_ONLY")
            put("includesFullTeam", includeFullTeam)
            put(
                "teamSnapshot",
                JSONObject().apply {
                    put("teamId", match.teamId)
                    put("name", match.teamName)
                    put("logoEmoji", teamSnapshot?.logoEmoji ?: "⚽")
                    put("logoUri", teamSnapshot?.logoUri ?: JSONObject.NULL)
                    put("shirtColorHex", match.shirtColorHex)
                    if (includeFullTeam) {
                        put("players", teamPlayersToJson(teamSnapshot))
                    }
                }
            )
            put(
                "match",
                JSONObject().apply {
                    put("originalMatchId", match.id)
                    put("teamId", match.teamId)
                    put("teamName", match.teamName)
                    put("shirtColorHex", match.shirtColorHex)
                    put("rivalName", match.rivalName)
                    put("matchDateLabel", match.matchDateLabel)
                    put("opponentGoals", match.opponentGoals)
                    put("opponentGoalChances", match.opponentGoalChances)
                    put("isStarted", match.isStarted)
                    put("isFinished", match.isFinished)
                    put("totalSeconds", match.totalSeconds)
                    put("remainingSeconds", match.remainingSeconds)
                    put("createdAtMillis", match.createdAtMillis)
                    put("finishedAtMillis", match.finishedAtMillis ?: JSONObject.NULL)
                    put("finishedAtLabel", match.finishedAtLabel)
                    put("starters", playersToJson(match.starters))
                    put("substitutes", playersToJson(match.substitutes))
                    put("expelledPlayers", playersToJson(match.expelledPlayers))
                    put("injuredPlayers", playersToJson(match.injuredPlayers))
                    put("events", matchEventsToJson(match.events))
                    put("playerTimes", playerTimesToJson(match.playerTimes))
                }
            )
        }

        return root.toString(2)
    }

    private fun parseImportedPlayers(array: JSONArray?): MutableList<Player> {
        val players = mutableListOf<Player>()
        if (array == null) return players

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id", index + 1)
            val name = item.optString("name").ifBlank { "Jugador ${index + 1}" }
            val number = item.optInt("number", index + 1)
            players.add(Player(id = id, name = name, number = number))
        }

        return players
    }

    private fun parseImportedEvents(array: JSONArray?): MutableList<MatchEvent> {
        val events = mutableListOf<MatchEvent>()
        if (array == null) return events

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val playerId = if (item.isNull("playerId")) null else item.optInt("playerId")
            events.add(
                MatchEvent(
                    minute = item.optInt("minute", 0),
                    type = item.optString("type").ifBlank { "Evento" },
                    playerId = playerId,
                    playerName = item.optString("playerName"),
                    detail = item.optString("detail"),
                    timestampLabel = item.optString("timestampLabel")
                )
            )
        }

        return events
    }

    private fun parseImportedPlayerTimes(array: JSONArray?): MutableMap<Int, MatchPlayerTime> {
        val playerTimes = mutableMapOf<Int, MatchPlayerTime>()
        if (array == null) return playerTimes

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val playerId = item.optInt("playerId", 0)
            if (playerId <= 0) continue

            playerTimes[playerId] = MatchPlayerTime(
                playerId = playerId,
                accumulatedSeconds = item.optInt("accumulatedSeconds", 0),
                isCurrentlyPlaying = item.optBoolean("isCurrentlyPlaying", false),
                lastEntrySecond = if (item.isNull("lastEntrySecond")) null else item.optInt("lastEntrySecond")
            )
        }

        return playerTimes
    }

    private fun collectImportedPlayers(matchJson: JSONObject): List<Player> {
        return buildList {
            addAll(parseImportedPlayers(matchJson.optJSONArray("starters")))
            addAll(parseImportedPlayers(matchJson.optJSONArray("substitutes")))
            addAll(parseImportedPlayers(matchJson.optJSONArray("expelledPlayers")))
            addAll(parseImportedPlayers(matchJson.optJSONArray("injuredPlayers")))
        }.distinctBy { "${it.name}_${it.number}" }
    }

    private suspend fun ensureTeamForImportedMatch(
        teamName: String,
        logoEmoji: String,
        logoUri: String?,
        shirtColorHex: String,
        players: List<Player>
    ): Team {
        val existing = teams.firstOrNull {
            it.name.trim().equals(teamName.trim(), ignoreCase = true) &&
                    it.shirtColorHex.equals(shirtColorHex, ignoreCase = true)
        }

        if (existing != null) {
            return repository.syncImportedPlayersToTeam(existing.id, players)
        }

        return repository.createImportedTeam(
            teamName = teamName,
            teamEmoji = logoEmoji,
            players = players,
            logoUri = logoUri,
            shirtColorHex = shirtColorHex
        )
    }

    private fun parseImportedMatch(
        matchJson: JSONObject,
        localTeamId: Int,
        fallbackTeamName: String,
        fallbackShirtColorHex: String
    ): MatchRecord {
        return MatchRecord(
            id = matchJson.optInt("originalMatchId", matchJson.optInt("id", 0)),
            teamId = localTeamId,
            teamName = matchJson.optString("teamName").ifBlank { fallbackTeamName },
            shirtColorHex = matchJson.optString("shirtColorHex").ifBlank { fallbackShirtColorHex },
            rivalName = matchJson.optString("rivalName"),
            matchDateLabel = matchJson.optString("matchDateLabel"),
            starters = parseImportedPlayers(matchJson.optJSONArray("starters")),
            substitutes = parseImportedPlayers(matchJson.optJSONArray("substitutes")),
            expelledPlayers = parseImportedPlayers(matchJson.optJSONArray("expelledPlayers")),
            injuredPlayers = parseImportedPlayers(matchJson.optJSONArray("injuredPlayers")),
            statsByPlayerId = mutableMapOf(),
            events = parseImportedEvents(matchJson.optJSONArray("events")),
            playerTimes = parseImportedPlayerTimes(matchJson.optJSONArray("playerTimes")),
            opponentGoals = matchJson.optInt("opponentGoals", 0),
            opponentGoalChances = matchJson.optInt("opponentGoalChances", 0),
            isStarted = matchJson.optBoolean("isStarted", false),
            isFinished = matchJson.optBoolean("isFinished", true),
            totalSeconds = matchJson.optInt("totalSeconds", 60),
            remainingSeconds = matchJson.optInt("remainingSeconds", 60),
            createdAtMillis = matchJson.optLong("createdAtMillis", System.currentTimeMillis()),
            finishedAtMillis = if (matchJson.isNull("finishedAtMillis")) null else matchJson.optLong("finishedAtMillis"),
            finishedAtLabel = matchJson.optString("finishedAtLabel")
        )
    }

    fun importMatchFromJson(
        jsonText: String,
        includeFullTeam: Boolean = false,
        onSuccess: (MatchRecord) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val rawJson = jsonText.trim()
        if (rawJson.isBlank()) {
            onError("Pega un JSON válido para importar.")
            return
        }

        val activeMatch = currentMatch
        if (activeMatch != null && activeMatch.isStarted && !activeMatch.isFinished) {
            onError("Finaliza el partido en curso antes de importar otro JSON.")
            return
        }

        viewModelScope.launch {
            try {
                val root = JSONObject(rawJson)
                val matchJson = when {
                    root.has("match") -> root.getJSONObject("match")
                    root.has("teamName") -> root
                    else -> throw IllegalArgumentException("No se encontró la información del partido en el JSON.")
                }

                val teamSnapshotJson = root.optJSONObject("teamSnapshot")
                val importedTeamName = teamSnapshotJson?.optString("name")
                    ?.takeIf { it.isNotBlank() }
                    ?: matchJson.optString("teamName").ifBlank { "Equipo Importado" }
                val importedShirtColor = teamSnapshotJson?.optString("shirtColorHex")
                    ?.takeIf { it.isNotBlank() }
                    ?: matchJson.optString("shirtColorHex").ifBlank { "#1E6B45" }
                val importedLogoEmoji = teamSnapshotJson?.optString("logoEmoji")
                    ?.takeIf { it.isNotBlank() }
                    ?: "⚽"
                val importedLogoUri = teamSnapshotJson?.optString("logoUri")
                    ?.takeIf { it.isNotBlank() && it != "null" }

                val importedPlayersFromMatch = collectImportedPlayers(matchJson)
                val importedPlayersFromTeam = parseImportedPlayers(teamSnapshotJson?.optJSONArray("players"))
                val playersForTeamImport = importedPlayersFromTeam.ifEmpty { importedPlayersFromMatch }

                val localTeam = if (includeFullTeam) {
                    ensureTeamForImportedMatch(
                        teamName = importedTeamName,
                        logoEmoji = importedLogoEmoji,
                        logoUri = importedLogoUri,
                        shirtColorHex = importedShirtColor,
                        players = playersForTeamImport
                    )
                } else {
                    selectedTeam ?: throw IllegalArgumentException(
                        "Selecciona primero un equipo si deseas importar solo el historial del partido."
                    )
                }

                var importedMatch = parseImportedMatch(
                    matchJson = matchJson,
                    localTeamId = localTeam.id,
                    fallbackTeamName = if (includeFullTeam) importedTeamName else localTeam.name,
                    fallbackShirtColorHex = if (includeFullTeam) importedShirtColor else localTeam.shirtColorHex
                )

                if (!includeFullTeam) {
                    importedMatch = importedMatch.copy(
                        teamId = localTeam.id,
                        teamName = localTeam.name,
                        shirtColorHex = localTeam.shirtColorHex
                    )
                }

                val savedMatch = repository.importMatch(importedMatch)
                putFinishedMatchLocally(savedMatch)
                selectedTeamId = localTeam.id
                onSuccess(savedMatch)
            } catch (e: Exception) {
                onError(e.message ?: "No se pudo importar el JSON.")
            }
        }
    }

    private fun putFinishedMatchLocally(match: MatchRecord) {
        val index = finishedMatches.indexOfFirst { it.id == match.id }
        if (index >= 0) {
            finishedMatches[index] = match
        } else {
            finishedMatches.add(0, match)
        }
    }

    private fun removeFinishedMatchLocally(matchId: Int) {
        val index = finishedMatches.indexOfFirst { it.id == matchId }
        if (index >= 0) {
            finishedMatches.removeAt(index)
        }
    }

    private fun getActiveMatchForStatsInternal(): MatchRecord? {
        return editingFinishedMatch ?: currentMatch
    }

    fun getActiveMatchForStats(): MatchRecord? {
        return getActiveMatchForStatsInternal()
    }

    fun isEditingFinishedMatchMode(): Boolean {
        return editingFinishedMatch != null
    }

    fun setEditChanges(changes: List<String>) {
        lastEditChanges = changes
        shouldShowEditResultDialog = true
    }

    fun dismissEditResultDialog() {
        shouldShowEditResultDialog = false
        lastEditChanges = emptyList()
    }

    fun updateFinishedMatch(updatedMatch: MatchRecord) {
        putFinishedMatchLocally(updatedMatch)

        if (selectedFinishedMatch?.id == updatedMatch.id) {
            selectedFinishedMatch = updatedMatch
        }

        if (editingFinishedMatch?.id == updatedMatch.id) {
            editingFinishedMatch = updatedMatch
        }

        if (currentMatch?.id == updatedMatch.id) {
            currentMatch = updatedMatch
        }

        viewModelScope.launch {
            repository.updateMatch(updatedMatch)
            repository.savePlayerTimes(updatedMatch)
        }
    }

    fun deleteFinishedMatch(matchId: Int) {
        removeFinishedMatchLocally(matchId)

        if (selectedFinishedMatch?.id == matchId) {
            selectedFinishedMatch = null
        }

        if (editingFinishedMatch?.id == matchId) {
            editingFinishedMatch = null
        }

        if (currentMatch?.id == matchId) {
            currentMatch = null
        }

        viewModelScope.launch {
            repository.deleteMatch(matchId)
        }
    }

    fun startFinishedMatch(match: MatchRecord) {
        editingFinishedMatch = match
    }

    fun startEditingPlayerFromFinishedMatch(match: MatchRecord, playerId: Int) {
        editingFinishedMatch = match
        selectedFinishedMatch = match
        selectedPlayerId = playerId

        val key = statsDraftKey(match.id, playerId)
        val initialDraft = buildDraftFromMatch(match, playerId)

        playerStatsDrafts[key] = initialDraft
        originalPlayerStatsDrafts[key] = initialDraft.copy()
    }

    fun clearEditingFinishedMatch() {
        editingFinishedMatch = null
    }

    private fun parseEventCount(event: MatchEvent): Int {
        val detailCount = event.detail.substringAfter(": ", "").toIntOrNull()
        return detailCount ?: 1
    }

    private fun allPlayersOf(match: MatchRecord): List<Player> {
        return (match.starters + match.substitutes + match.expelledPlayers + match.injuredPlayers)
            .distinctBy { it.id }
    }

    private fun calculateOpponentGoalChancesFromEvents(
        events: List<MatchEvent>,
        match: MatchRecord
    ): Int {
        return events.sumOf { event ->
            if (isOpponentGoalChanceType(event.type, match)) parseEventCount(event) else 0
        }
    }

    private fun buildElapsedMatchEvent(
        match: MatchRecord,
        type: String,
        playerName: String,
        playerId: Int? = null,
        count: Int = 1,
        detail: String = "$type: $count"
    ): MatchEvent {
        return MatchEvent(
            minute = elapsedSeconds(match) / 60,
            type = type,
            playerId = playerId,
            playerName = playerName,
            detail = detail,
            timestampLabel = getElapsedMatchTimeLabel(match)
        )
    }

    private fun currentTeamLabel(match: MatchRecord?): String {
        return match?.teamName?.ifBlank { "Mi Equipo" } ?: "Mi Equipo"
    }

    private fun currentRivalLabel(match: MatchRecord?): String {
        return match?.rivalName?.ifBlank { "Equipo Rival" } ?: "Equipo Rival"
    }

    private fun teamGoalEventType(match: MatchRecord?): String = "Gol ${currentTeamLabel(match)}"
    private fun opponentGoalEventType(match: MatchRecord?): String = "Gol ${currentRivalLabel(match)}"
    private fun teamAssistEventType(match: MatchRecord?): String = "Participación Gol ${currentTeamLabel(match)}"
    private fun opponentAssistEventType(match: MatchRecord?): String = "Participación Gol ${currentRivalLabel(match)}"
    private fun teamFoulEventType(match: MatchRecord?): String = "Falta para ${currentTeamLabel(match)}"
    private fun opponentFoulEventType(match: MatchRecord?): String = "Falta para ${currentRivalLabel(match)}"
    private fun teamOffsideEventType(match: MatchRecord?): String = "Off Side para ${currentTeamLabel(match)}"
    private fun opponentOffsideEventType(match: MatchRecord?): String = "Off Side para ${currentRivalLabel(match)}"
    private fun teamPenaltyEventType(match: MatchRecord?): String = "Penal para ${currentTeamLabel(match)}"
    private fun opponentPenaltyEventType(match: MatchRecord?): String = "Penal para ${currentRivalLabel(match)}"
    private fun opponentGoalChanceEventType(match: MatchRecord?): String = "Oportunidad de Gol ${currentRivalLabel(match)}"

    private fun isTeamGoalType(type: String, match: MatchRecord?): Boolean {
        val teamName = currentTeamLabel(match)
        return type == "Gol a Favor" || type == "Gol $teamName" || type == "Gol de $teamName"
    }

    private fun isOpponentGoalType(type: String, match: MatchRecord?): Boolean {
        val rivalName = currentRivalLabel(match)
        return type == "Gol en Contra" ||
            type == "Gol Rival" ||
            type == "Gol $rivalName" ||
            type == "Gol de $rivalName"
    }

    private fun isTeamAssistType(type: String, match: MatchRecord?): Boolean {
        return type == "Asistencia a favor" || type == teamAssistEventType(match)
    }

    private fun isOpponentAssistType(type: String, match: MatchRecord?): Boolean {
        return type == "Asistencia en contra" || type == opponentAssistEventType(match)
    }

    private fun isTeamFoulType(type: String, match: MatchRecord?): Boolean {
        return type == "Falta a Favor" || type == teamFoulEventType(match)
    }

    private fun isOpponentFoulType(type: String, match: MatchRecord?): Boolean {
        return type == "Falta en Contra" || type == opponentFoulEventType(match)
    }

    private fun isTeamOffsideType(type: String, match: MatchRecord?): Boolean {
        return type == "Tiro Libre a Favor" || type == "Off Side a Favor" || type == teamOffsideEventType(match)
    }

    private fun isOpponentOffsideType(type: String, match: MatchRecord?): Boolean {
        return type == "Tiro Libre en Contra" || type == "Off Side en Contra" || type == opponentOffsideEventType(match)
    }

    private fun isTeamPenaltyType(type: String, match: MatchRecord?): Boolean {
        return type == "Penal a Favor" || type == teamPenaltyEventType(match)
    }

    private fun isOpponentPenaltyType(type: String, match: MatchRecord?): Boolean {
        return type == "Penal en Contra" || type == opponentPenaltyEventType(match)
    }

    private fun isOpponentGoalChanceType(type: String, match: MatchRecord?): Boolean {
        return type == "Oportunidad de Gol Rival" || type == opponentGoalChanceEventType(match)
    }

    private fun buildDraftFromMatch(match: MatchRecord, playerId: Int): PlayerStatsDraft {
        val player = allPlayersOf(match).firstOrNull { it.id == playerId }
            ?: return PlayerStatsDraft(playerId = playerId, matchId = match.id)

        val playerEvents = match.events.filter { it.playerId == player.id }

        var draft = PlayerStatsDraft(playerId = playerId, matchId = match.id)

        playerEvents.forEach { event ->
            val count = parseEventCount(event)
            val type = event.type

            draft = when {
                isTeamGoalType(type, match) -> draft.copy(golFavor = draft.golFavor + count)
                isOpponentGoalType(type, match) -> draft.copy(golContra = draft.golContra + count)
                type == "Tiro al Arco +" -> draft.copy(tiroAlArcoPositivo = draft.tiroAlArcoPositivo + count)
                type == "Tiro al Arco -" -> draft.copy(tiroAlArcoNegativo = draft.tiroAlArcoNegativo + count)
                isTeamAssistType(type, match) -> draft.copy(participacionGolFavor = draft.participacionGolFavor + count)
                isOpponentAssistType(type, match) -> draft.copy(participacionGolContra = draft.participacionGolContra + count)
                type == "Remate 1/2 +" -> draft.copy(remate12Positivo = draft.remate12Positivo + count)
                type == "Remate 1/2 -" -> draft.copy(remate12Negativo = draft.remate12Negativo + count)

                type == "Balón Recup." || type == "Balón Recogido a Favor" -> draft.copy(balonRecogidoFavor = draft.balonRecogidoFavor + count)
                type == "Balón Perdido" || type == "Balón Recogido en Contra" -> draft.copy(balonRecogidoContra = draft.balonRecogidoContra + count)
                type == "Pases Buenos" -> draft.copy(pasesBuenos = draft.pasesBuenos + count)
                type == "Pases Malos" -> draft.copy(pasesMalos = draft.pasesMalos + count)
                type == "Centros +" -> draft.copy(centrosPositivos = draft.centrosPositivos + count)
                type == "Centros -" -> draft.copy(centrosNegativos = draft.centrosNegativos + count)
                type == "Rechazos +" -> draft.copy(rechazosPositivos = draft.rechazosPositivos + count)
                type == "Rechazos -" -> draft.copy(rechazosNegativos = draft.rechazosNegativos + count)

                isTeamFoulType(type, match) -> draft.copy(faltaFavor = draft.faltaFavor + count)
                isOpponentFoulType(type, match) -> draft.copy(faltaContra = draft.faltaContra + count)
                type == "Corner +" -> draft.copy(cornerPositivo = draft.cornerPositivo + count)
                type == "Corner -" -> draft.copy(cornerNegativo = draft.cornerNegativo + count)
                isTeamOffsideType(type, match) -> draft.copy(tiroLibreFavor = draft.tiroLibreFavor + count)
                isOpponentOffsideType(type, match) -> draft.copy(tiroLibreContra = draft.tiroLibreContra + count)
                isTeamPenaltyType(type, match) -> draft.copy(penalFavor = draft.penalFavor + count)
                isOpponentPenaltyType(type, match) -> draft.copy(penalContra = draft.penalContra + count)

                type == "Amarilla" -> draft.copy(amarilla = draft.amarilla + count)
                type == "Roja" -> draft.copy(roja = draft.roja + count)

                type == "Doble Amarilla" -> draft.copy(amarilla = maxOf(draft.amarilla, 2))
                else -> draft
            }
        }

        return draft
    }


    fun getOrCreatePlayerStatsDraft(playerId: Int): PlayerStatsDraft {
        val match = getActiveMatchForStatsInternal()
            ?: return PlayerStatsDraft(playerId = playerId, matchId = 0)

        val key = statsDraftKey(match.id, playerId)

        return playerStatsDrafts.getOrPut(key) {
            val initialDraft = if (editingFinishedMatch?.id == match.id) {
                buildDraftFromMatch(match, playerId)
            } else {
                PlayerStatsDraft(playerId = playerId, matchId = match.id)
            }

            if (editingFinishedMatch?.id == match.id) {
                originalPlayerStatsDrafts.putIfAbsent(key, initialDraft.copy())
            }

            initialDraft
        }
    }

    private fun createEventFromDraft(
        player: Player,
        type: String,
        count: Int,
        timestampLabel: String,
        minute: Int
    ): MatchEvent {
        return MatchEvent(
            minute = minute,
            type = type,
            playerId = player.id,
            playerName = player.name,
            detail = "$type: $count",
            timestampLabel = timestampLabel
        )
    }

    private fun buildEventsFromDraftForPlayer(
        match: MatchRecord,
        player: Player,
        draft: PlayerStatsDraft,
        oldEventsOfPlayer: List<MatchEvent>,
        defaultTimestamp: String
    ): List<MatchEvent> {
        val result = mutableListOf<MatchEvent>()

        fun oldEvent(vararg types: String) = oldEventsOfPlayer.firstOrNull { current ->
            types.any { it == current.type }
        }

        fun addIfNeeded(type: String, count: Int, vararg legacyTypes: String) {
            if (count <= 0) return
            val previous = oldEvent(type, *legacyTypes)
            result.add(
                createEventFromDraft(
                    player = player,
                    type = type,
                    count = count,
                    timestampLabel = previous?.timestampLabel ?: defaultTimestamp,
                    minute = previous?.minute ?: getCurrentMatchMinute(match)
                )
            )
        }

        addIfNeeded(teamGoalEventType(match), draft.golFavor, "Gol a Favor")
        addIfNeeded(opponentGoalEventType(match), draft.golContra, "Gol en Contra")
        addIfNeeded("Tiro al Arco +", draft.tiroAlArcoPositivo)
        addIfNeeded("Tiro al Arco -", draft.tiroAlArcoNegativo)
        addIfNeeded(teamAssistEventType(match), draft.participacionGolFavor, "Asistencia a favor")
        addIfNeeded(opponentAssistEventType(match), draft.participacionGolContra, "Asistencia en contra")
        addIfNeeded("Remate 1/2 +", draft.remate12Positivo)
        addIfNeeded("Remate 1/2 -", draft.remate12Negativo)

        addIfNeeded("Balón Recogido a Favor", draft.balonRecogidoFavor, "Balón Recup.")
        addIfNeeded("Balón Recogido en Contra", draft.balonRecogidoContra, "Balón Perdido")
        addIfNeeded("Pases Buenos", draft.pasesBuenos)
        addIfNeeded("Pases Malos", draft.pasesMalos)
        addIfNeeded("Centros +", draft.centrosPositivos)
        addIfNeeded("Centros -", draft.centrosNegativos)
        addIfNeeded("Rechazos +", draft.rechazosPositivos)
        addIfNeeded("Rechazos -", draft.rechazosNegativos)

        addIfNeeded(teamFoulEventType(match), draft.faltaFavor, "Falta a Favor")
        addIfNeeded(opponentFoulEventType(match), draft.faltaContra, "Falta en Contra")
        addIfNeeded("Corner +", draft.cornerPositivo)
        addIfNeeded("Corner -", draft.cornerNegativo)
        addIfNeeded(teamOffsideEventType(match), draft.tiroLibreFavor, "Tiro Libre a Favor", "Off Side a Favor")
        addIfNeeded(opponentOffsideEventType(match), draft.tiroLibreContra, "Tiro Libre en Contra", "Off Side en Contra")
        addIfNeeded(teamPenaltyEventType(match), draft.penalFavor, "Penal a Favor")
        addIfNeeded(opponentPenaltyEventType(match), draft.penalContra, "Penal en Contra")

        addIfNeeded("Amarilla", draft.amarilla)

        if (draft.amarilla >= 2) {
            addIfNeeded("Doble Amarilla", 1)
        }

        if (draft.roja > 0) {
            addIfNeeded("Roja", 1)
        }

        return result
    }
    private fun buildEditChanges(
        playerName: String,
        original: PlayerStatsDraft,
        updated: PlayerStatsDraft,
        match: MatchRecord
    ): List<String> {
        val changes = mutableListOf<String>()

        fun compare(label: String, oldValue: Int, newValue: Int) {
            if (oldValue != newValue) {
                changes.add("$playerName · $label: $oldValue → $newValue")
            }
        }

        compare(teamGoalEventType(match), original.golFavor, updated.golFavor)
        compare(opponentGoalEventType(match), original.golContra, updated.golContra)
        compare("Tiro al Arco +", original.tiroAlArcoPositivo, updated.tiroAlArcoPositivo)
        compare("Tiro al Arco -", original.tiroAlArcoNegativo, updated.tiroAlArcoNegativo)
        compare(teamAssistEventType(match), original.participacionGolFavor, updated.participacionGolFavor)
        compare(opponentAssistEventType(match), original.participacionGolContra, updated.participacionGolContra)
        compare("Remate 1/2 +", original.remate12Positivo, updated.remate12Positivo)
        compare("Remate 1/2 -", original.remate12Negativo, updated.remate12Negativo)

        compare("Balón Recogido a Favor", original.balonRecogidoFavor, updated.balonRecogidoFavor)
        compare("Balón Recogido en Contra", original.balonRecogidoContra, updated.balonRecogidoContra)
        compare("Pases Buenos", original.pasesBuenos, updated.pasesBuenos)
        compare("Pases Malos", original.pasesMalos, updated.pasesMalos)
        compare("Centros +", original.centrosPositivos, updated.centrosPositivos)
        compare("Centros -", original.centrosNegativos, updated.centrosNegativos)
        compare("Rechazos +", original.rechazosPositivos, updated.rechazosPositivos)
        compare("Rechazos -", original.rechazosNegativos, updated.rechazosNegativos)

        compare(teamFoulEventType(match), original.faltaFavor, updated.faltaFavor)
        compare(opponentFoulEventType(match), original.faltaContra, updated.faltaContra)
        compare("Corner +", original.cornerPositivo, updated.cornerPositivo)
        compare("Corner -", original.cornerNegativo, updated.cornerNegativo)
        compare(teamOffsideEventType(match), original.tiroLibreFavor, updated.tiroLibreFavor)
        compare(opponentOffsideEventType(match), original.tiroLibreContra, updated.tiroLibreContra)
        compare(teamPenaltyEventType(match), original.penalFavor, updated.penalFavor)
        compare(opponentPenaltyEventType(match), original.penalContra, updated.penalContra)

        compare("Amarilla", original.amarilla, updated.amarilla)
        compare("Roja", original.roja, updated.roja)

        return changes
    }

    private fun saveEditedFinishedMatch(playerId: Int): List<String> {
        val match = editingFinishedMatch ?: return emptyList()
        val player = allPlayersOf(match).firstOrNull { it.id == playerId } ?: return emptyList()

        val key = statsDraftKey(match.id, playerId)
        val updatedDraft = playerStatsDrafts[key] ?: return emptyList()
        val originalDraft = originalPlayerStatsDrafts[key] ?: buildDraftFromMatch(match, playerId)

        val changes = buildEditChanges(player.name, originalDraft, updatedDraft, match)

        val oldEventsOfPlayer = match.events.filter { it.playerId == playerId }
        val eventsWithoutPlayer = match.events.filterNot { it.playerId == playerId }.toMutableList()

        val rebuiltEventsForPlayer = buildEventsFromDraftForPlayer(
            match = match,
            player = player,
            draft = updatedDraft,
            oldEventsOfPlayer = oldEventsOfPlayer,
            defaultTimestamp = match.finishedAtLabel.ifBlank { "00:00" }
        )

        val rebuiltEvents = (eventsWithoutPlayer + rebuiltEventsForPlayer).toMutableList()

        val updatedMatch = match.copy(
            events = rebuiltEvents,
            opponentGoals = calculateOpponentGoalsFromEvents(rebuiltEvents, match),
            opponentGoalChances = calculateOpponentGoalChancesFromEvents(rebuiltEvents, match)
        )

        updateFinishedMatch(updatedMatch)
        originalPlayerStatsDrafts[key] = updatedDraft.copy()
        setEditChanges(changes)
        editingFinishedMatch = updatedMatch
        selectedFinishedMatch = updatedMatch

        viewModelScope.launch {
            repository.replacePlayerEvents(updatedMatch.id, playerId, rebuiltEventsForPlayer)
        }

        return changes
    }

    private fun getCurrentMatchMinute(match: MatchRecord): Int {
        val elapsed = (match.totalSeconds - match.remainingSeconds).coerceAtLeast(0)
        return (elapsed / 60).coerceAtLeast(0)
    }

    private fun formatMatchClock(match: MatchRecord): String {
        val elapsed = (match.totalSeconds - match.remainingSeconds).coerceAtLeast(0)
        val minutes = elapsed / 60
        val seconds = elapsed % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    fun clearPlayerStatsDraft(playerId: Int) {
        val match = getActiveMatchForStats() ?: return
        val key = statsDraftKey(match.id, playerId)
        playerStatsDrafts.remove(key)
    }

    fun savePlayerStatsDraftAsEvents(playerId: Int): List<String> {
        if (isEditingFinishedMatchMode()) {
            return saveEditedFinishedMatch(playerId)
        }

        val match = getActiveMatchForStats() ?: return emptyList()
        val draft = getOrCreatePlayerStatsDraft(playerId)
        val player = allPlayersOf(match)
            .firstOrNull { it.id == playerId }
            ?: return emptyList()

        val oldEventsOfPlayer = match.events.filter { it.playerId == playerId }
        val eventsWithoutPlayer = match.events.filterNot { it.playerId == playerId }.toMutableList()

        val rebuiltEventsForPlayer = buildEventsFromDraftForPlayer(
            match = match,
            player = player,
            draft = draft,
            oldEventsOfPlayer = oldEventsOfPlayer,
            defaultTimestamp = formatMatchClock(match)
        )

        val rebuiltEvents = (eventsWithoutPlayer + rebuiltEventsForPlayer)
            .sortedBy { it.minute }
            .toMutableList()

        val updatedMatch = match.copy(
            events = rebuiltEvents,
            opponentGoals = calculateOpponentGoalsFromEvents(rebuiltEvents, match),
            opponentGoalChances = calculateOpponentGoalChancesFromEvents(rebuiltEvents, match)
        )
        currentMatch = updatedMatch

        val changes = mutableListOf<String>()

        fun addChange(label: String, value: Int) {
            if (value > 0) changes.add("${player.name}: $label x$value")
        }

        addChange(teamGoalEventType(match), draft.golFavor)
        addChange(opponentGoalEventType(match), draft.golContra)
        addChange("Tiro al Arco +", draft.tiroAlArcoPositivo)
        addChange("Tiro al Arco -", draft.tiroAlArcoNegativo)
        addChange(teamAssistEventType(match), draft.participacionGolFavor)
        addChange(opponentAssistEventType(match), draft.participacionGolContra)
        addChange("Remate 1/2 +", draft.remate12Positivo)
        addChange("Remate 1/2 -", draft.remate12Negativo)

        addChange("Balón Recogido a Favor", draft.balonRecogidoFavor)
        addChange("Balón Recogido en Contra", draft.balonRecogidoContra)
        addChange("Pases Buenos", draft.pasesBuenos)
        addChange("Pases Malos", draft.pasesMalos)
        addChange("Centros +", draft.centrosPositivos)
        addChange("Centros -", draft.centrosNegativos)
        addChange("Rechazos +", draft.rechazosPositivos)
        addChange("Rechazos -", draft.rechazosNegativos)

        addChange(teamFoulEventType(match), draft.faltaFavor)
        addChange(opponentFoulEventType(match), draft.faltaContra)
        addChange("Corner +", draft.cornerPositivo)
        addChange("Corner -", draft.cornerNegativo)
        addChange(teamOffsideEventType(match), draft.tiroLibreFavor)
        addChange(opponentOffsideEventType(match), draft.tiroLibreContra)
        addChange(teamPenaltyEventType(match), draft.penalFavor)
        addChange(opponentPenaltyEventType(match), draft.penalContra)

        addChange("Amarilla", draft.amarilla)
        addChange("Roja", draft.roja)

        viewModelScope.launch {
            repository.replacePlayerEvents(updatedMatch.id, playerId, rebuiltEventsForPlayer)
            repository.updateMatch(updatedMatch)
        }

        val rebuiltDraft = buildDraftFromMatch(updatedMatch, playerId)
        updatePlayerStatsDraft(rebuiltDraft)

        return changes
    }

    fun updatePlayerStatsDraft(updatedDraft: PlayerStatsDraft) {
        val key = statsDraftKey(updatedDraft.matchId, updatedDraft.playerId)
        playerStatsDrafts[key] = updatedDraft
    }


    private fun resetPauseUiState() {
        matchPauseState = MatchPauseState.NONE
    }

    private fun resetMatchControlState() {
        matchPauseState = MatchPauseState.NONE
        firstHalfRegistered = false
        secondHalfStarted = false
    }

    private fun syncPauseStateWithMatch(match: MatchRecord?) {
        if (match == null || !match.isStarted || match.isFinished) {
            resetMatchControlState()
        }
    }

    fun pauseMatch() {
        val match = currentMatch ?: return
        if (!match.isStarted || match.isFinished) return
        matchPauseState = MatchPauseState.MANUAL
    }

    fun pauseAtFirstHalf() {
        val match = currentMatch ?: return
        if (!match.isStarted || match.isFinished) return
        if (firstHalfRegistered) return
        firstHalfRegistered = true
        matchPauseState = MatchPauseState.FIRST_HALF
    }

    fun resumeMatch() {
        val match = currentMatch ?: return
        if (!match.isStarted || match.isFinished) return
        if (matchPauseState == MatchPauseState.FIRST_HALF && firstHalfRegistered) {
            secondHalfStarted = true
        }
        matchPauseState = MatchPauseState.NONE
    }

    fun toggleMatchPause() {
        val match = currentMatch ?: return
        if (!match.isStarted || match.isFinished) return
        matchPauseState = if (isMatchPaused) MatchPauseState.NONE else MatchPauseState.MANUAL
    }

    fun isFirstHalfPaused(): Boolean {
        return matchPauseState == MatchPauseState.FIRST_HALF
    }

    fun hasFirstHalfActionAvailable(): Boolean {
        return !firstHalfRegistered
    }

    fun getCurrentPeriodLabel(): String {
        return if (secondHalfStarted) "SEGUNDO TIEMPO" else "PRIMER TIEMPO"
    }

    fun getPauseBannerTitle(): String {
        return when (matchPauseState) {
            MatchPauseState.FIRST_HALF -> "Primer tiempo finalizado"
            MatchPauseState.MANUAL -> "Partido pausado"
            MatchPauseState.NONE -> "Partido en curso"
        }
    }

    fun getPauseBannerSubtitle(): String {
        return when (matchPauseState) {
            MatchPauseState.FIRST_HALF -> "Pulsa PLAY y selecciona Reanudar para iniciar el segundo tiempo"
            MatchPauseState.MANUAL -> "Pulsa PLAY y selecciona Reanudar para continuar"
            MatchPauseState.NONE -> ""
        }
    }

    fun setMatchDuration(minutes: Int) {
        val safeMinutes = minutes.coerceIn(10, 90)
        setMatchDurationSeconds(safeMinutes * 60)
    }

    fun setMatchDurationSeconds(totalSeconds: Int) {
        val match = currentMatch ?: return
        val safeTotalSeconds = totalSeconds.coerceAtLeast(30)
        val updatedMatch = match.copy(
            totalSeconds = safeTotalSeconds,
            remainingSeconds = safeTotalSeconds
        )
        currentMatch = updatedMatch
        resetMatchControlState()
        viewModelScope.launch { repository.updateMatch(updatedMatch) }
    }

    fun setMatchRivalAndDate(rivalName: String, matchDateLabel: String) {
        val match = currentMatch ?: return
        val updatedMatch = match.copy(
            rivalName = rivalName,
            matchDateLabel = matchDateLabel
        )
        currentMatch = updatedMatch
        viewModelScope.launch { repository.updateMatch(updatedMatch) }
    }

    fun updateOpponentGoals(delta: Int) {
        val match = currentMatch ?: return
        if (delta == 0) return

        val updatedEvents = match.events.toMutableList()
        val rivalName = currentRivalLabel(match)
        val opponentGoalType = opponentGoalEventType(match)

        if (delta > 0) {
            repeat(delta) {
                updatedEvents.add(
                    buildElapsedMatchEvent(
                        match = match,
                        type = opponentGoalType,
                        playerName = rivalName
                    )
                )
            }
        } else {
            repeat(-delta) {
                decreaseOrRemoveLastEventOfType(updatedEvents, opponentGoalType, "Gol Rival")
            }
        }

        val updatedMatch = match.copy(
            events = updatedEvents,
            opponentGoals = calculateOpponentGoalsFromEvents(updatedEvents, match)
        )

        currentMatch = updatedMatch

        viewModelScope.launch {
            repository.replaceAllMatchEvents(updatedMatch.id, updatedEvents)
            repository.updateMatch(updatedMatch)
        }
    }

    fun updateOpponentGoalChances(delta: Int) {
        val match = currentMatch ?: return
        if (delta == 0) return

        val updatedEvents = match.events.toMutableList()
        val rivalName = currentRivalLabel(match)
        val opponentChanceType = opponentGoalChanceEventType(match)

        if (delta > 0) {
            repeat(delta) {
                updatedEvents.add(
                    buildElapsedMatchEvent(
                        match = match,
                        type = opponentChanceType,
                        playerName = rivalName
                    )
                )
            }
        } else {
            repeat(-delta) {
                decreaseOrRemoveLastEventOfType(updatedEvents, opponentChanceType, "Oportunidad de Gol Rival")
            }
        }

        val updatedMatch = match.copy(
            events = updatedEvents,
            opponentGoalChances = calculateOpponentGoalChancesFromEvents(updatedEvents, match)
        )

        currentMatch = updatedMatch

        viewModelScope.launch {
            repository.replaceAllMatchEvents(updatedMatch.id, updatedEvents)
            repository.updateMatch(updatedMatch)
        }
    }

    private fun calculateOpponentGoalsFromEvents(
        events: List<MatchEvent>,
        match: MatchRecord? = currentMatch
    ): Int {
        val safeMatch = match ?: return 0
        return events.sumOf { event ->
            if (isOpponentGoalType(event.type, safeMatch)) parseEventCount(event) else 0
        }
    }

    fun getBlockedOpponentGoalRemovalMessage(): String? {
        val match = currentMatch ?: return null

        val hasManualRivalGoal = match.events.any { event ->
            event.playerId == null && (event.type == "Gol Rival" || event.type == opponentGoalEventType(match))
        }
        if (hasManualRivalGoal) return null

        val lastOwnGoal = match.events.lastOrNull { event ->
            event.playerId != null && isOpponentGoalType(event.type, match)
        }
        return if (lastOwnGoal != null) {
            "No se puede eliminar: Gol en contra por ${lastOwnGoal.playerName}"
        } else {
            null
        }
    }

    private fun decreaseOrRemoveLastEventOfType(
        events: MutableList<MatchEvent>,
        type: String,
        vararg legacyTypes: String
    ): Boolean {
        val acceptedTypes = setOf(type, *legacyTypes)
        val index = events.indexOfLast { it.type in acceptedTypes }
        if (index == -1) return false

        val target = events[index]
        val currentCount = parseEventCount(target)

        if (currentCount > 1) {
            events[index] = target.copy(detail = "$type: ${currentCount - 1}")
        } else {
            events.removeAt(index)
        }

        return true
    }

    fun selectTeam(team: Team) {
        selectedTeamId = team.id
    }

    fun addPlayer(name: String, number: Int) {
        val team = selectedTeam ?: return
        if (name.isBlank() || team.players.size >= 30) return
        if (number <= 0) return
        if (team.players.any { it.number == number }) return

        viewModelScope.launch {
            repository.addPlayer(team.id, name, number)
        }
    }

    fun selectFinishedMatch(match: MatchRecord) {
        selectedFinishedMatch = finishedMatches.firstOrNull { it.id == match.id } ?: match
    }

    fun dismissFinishedDialog() {
        shouldShowFinishedDialog = false
    }

    fun removePlayer(player: Player) {
        val team = selectedTeam ?: return
        viewModelScope.launch { repository.removePlayer(player.id) }
    }

    fun expelPlayerByCard(player: Player, reason: String) {
        val match = currentMatch ?: return
        if (!match.isStarted || match.isFinished) return

        val cleanedMatch = removePlayerFromAllLists(match, player.id)

        val updatedMatch = cleanedMatch.copy(
            expelledPlayers = cleanedMatch.expelledPlayers.toMutableList().apply {
                if (none { it.id == player.id }) {
                    add(player)
                }
            }
        )

        currentMatch = updatedMatch

        viewModelScope.launch {
            repository.saveMatchAndLineup(updatedMatch)
        }
    }

    fun createNewMatch(onCreated: (() -> Unit)? = null) {
        val team = selectedTeam ?: return
        clearEditingFinishedMatch()
        selectedFinishedMatch = null
        selectedPlayerId = null
        matchTimerJob?.cancel()
        resetMatchControlState()
        viewModelScope.launch {
            val newMatch = repository.createNewMatch(team)
            currentMatch = newMatch
            onCreated?.invoke()
        }
    }

    fun selectPlayerForStats(playerId: Int) {
        selectedPlayerId = playerId
    }

    fun getSelectedPlayer(): Player? {
        val match = getActiveMatchForStatsInternal() ?: return null
        val id = selectedPlayerId ?: return null
        return allPlayersOf(match).firstOrNull { it.id == id }
    }

    private fun getElapsedMatchTimeLabel(match: MatchRecord): String {
        val elapsed = (match.totalSeconds - match.remainingSeconds).coerceAtLeast(0)
        val minutes = elapsed / 60
        val seconds = elapsed % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun elapsedSeconds(match: MatchRecord): Int {
        return (match.totalSeconds - match.remainingSeconds).coerceAtLeast(0)
    }

    private fun initializePlayerTimesForMatch(match: MatchRecord): MutableMap<Int, MatchPlayerTime> {
        val allPlayers = (match.starters + match.substitutes).distinctBy { it.id }
        return allPlayers.associate { player ->
            val isStarter = match.starters.any { it.id == player.id }
            player.id to MatchPlayerTime(
                playerId = player.id,
                accumulatedSeconds = 0,
                isCurrentlyPlaying = isStarter,
                lastEntrySecond = if (isStarter) 0 else null
            )
        }.toMutableMap()
    }

    private fun finalizePlayingTimes(match: MatchRecord): MutableMap<Int, MatchPlayerTime> {
        val elapsed = elapsedSeconds(match)
        return match.playerTimes.mapValues { (_, playerTime) ->
            if (playerTime.isCurrentlyPlaying) {
                val entry = playerTime.lastEntrySecond ?: 0
                playerTime.copy(
                    accumulatedSeconds = playerTime.accumulatedSeconds + (elapsed - entry).coerceAtLeast(0),
                    isCurrentlyPlaying = false,
                    lastEntrySecond = null
                )
            } else {
                playerTime
            }
        }.toMutableMap()
    }

    private fun buildFinishedMatch(match: MatchRecord): MatchRecord {
        val finalizedPlayerTimes = finalizePlayingTimes(match)
        return match.copy(
            playerTimes = finalizedPlayerTimes,
            isStarted = false,
            isFinished = true,
            finishedAtMillis = System.currentTimeMillis(),
            finishedAtLabel = getElapsedMatchTimeLabel(match),
            opponentGoals = calculateOpponentGoalsFromEvents(match.events, match),
            opponentGoalChances = calculateOpponentGoalChancesFromEvents(match.events, match)
        )
    }

    fun getDisplayedPlayerSeconds(playerId: Int, match: MatchRecord? = currentMatch): Int {
        val safeMatch = match ?: return 0
        val playerTime = safeMatch.playerTimes[playerId] ?: return 0
        return if (playerTime.isCurrentlyPlaying) {
            val elapsed = elapsedSeconds(safeMatch)
            val entry = playerTime.lastEntrySecond ?: 0
            playerTime.accumulatedSeconds + (elapsed - entry).coerceAtLeast(0)
        } else {
            playerTime.accumulatedSeconds
        }
    }

    fun getFormattedPlayerTime(playerId: Int, match: MatchRecord? = currentMatch): String {
        val totalSeconds = getDisplayedPlayerSeconds(playerId, match)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun getCurrentStarters(match: MatchRecord? = currentMatch): List<Player> {
        val safeMatch = match ?: return emptyList()

        if (!safeMatch.isStarted || safeMatch.isFinished) {
            return safeMatch.starters.sortedBy { it.number }
        }

        val allSelectedPlayers = (safeMatch.starters + safeMatch.substitutes).distinctBy { it.id }
        val currentPlayingIds = safeMatch.playerTimes
            .values
            .filter { it.isCurrentlyPlaying }
            .map { it.playerId }
            .toSet()

        return allSelectedPlayers
            .filter { it.id in currentPlayingIds }
            .sortedBy { it.number }
    }

    fun getCurrentSubstitutes(match: MatchRecord? = currentMatch): List<Player> {
        val safeMatch = match ?: return emptyList()

        if (!safeMatch.isStarted || safeMatch.isFinished) {
            return safeMatch.substitutes.sortedBy { it.number }
        }

        val allSelectedPlayers = (safeMatch.starters + safeMatch.substitutes).distinctBy { it.id }
        val currentPlayingIds = safeMatch.playerTimes
            .values
            .filter { it.isCurrentlyPlaying }
            .map { it.playerId }
            .toSet()

        return allSelectedPlayers
            .filter { it.id !in currentPlayingIds }
            .sortedBy { it.number }
    }

    fun stopMatch() {
        val match = currentMatch ?: return
        matchTimerJob?.cancel()
        resetPauseUiState()

        val finishedMatch = buildFinishedMatch(match)

        currentMatch = finishedMatch
        putFinishedMatchLocally(finishedMatch)
        shouldShowFinishedDialog = true

        viewModelScope.launch {
            repository.saveMatchAndLineup(finishedMatch)
            repository.replaceAllMatchEvents(finishedMatch.id, finishedMatch.events)
        }
    }

    fun getElapsedMatchTimeLabel(): String {
        val match = currentMatch ?: return "00:00"
        return getElapsedMatchTimeLabel(match)
    }

    fun getTotalEventsOfCurrentMatch() = currentMatch?.events?.size ?: 0
    fun getTotalEventsOfSelectedFinishedMatch() = selectedFinishedMatch?.events?.size ?: 0

    private fun removePlayerFromAllLists(match: MatchRecord, playerId: Int): MatchRecord {
        return match.copy(
            starters = match.starters.filterNot { it.id == playerId }.toMutableList(),
            substitutes = match.substitutes.filterNot { it.id == playerId }.toMutableList(),
            expelledPlayers = match.expelledPlayers.filterNot { it.id == playerId }.toMutableList(),
            injuredPlayers = match.injuredPlayers.filterNot { it.id == playerId }.toMutableList()
        )
    }

    fun toggleStarter(player: Player) {
        val match = currentMatch ?: return
        if (match.isStarted || match.isFinished) return

        val cleanedMatch = removePlayerFromAllLists(match, player.id)
        val updatedStarters = cleanedMatch.starters.toMutableList()
        val exists = match.starters.any { it.id == player.id }

        if (!exists && updatedStarters.size < 11) {
            updatedStarters.add(player)
        }

        val updatedMatch = cleanedMatch.copy(
            starters = if (exists) cleanedMatch.starters else updatedStarters
        )
        currentMatch = updatedMatch
        viewModelScope.launch { repository.saveMatchAndLineup(updatedMatch) }
    }

    fun toggleSubstitute(player: Player) {
        val match = currentMatch ?: return
        if (match.isStarted || match.isFinished) return

        val cleanedMatch = removePlayerFromAllLists(match, player.id)
        val exists = match.substitutes.any { it.id == player.id }

        val updatedMatch = cleanedMatch.copy(
            substitutes = if (exists) cleanedMatch.substitutes else cleanedMatch.substitutes.toMutableList().apply { add(player) }
        )
        currentMatch = updatedMatch
        viewModelScope.launch { repository.saveMatchAndLineup(updatedMatch) }
    }

    fun toggleExpelled(player: Player) {
        val match = currentMatch ?: return
        if (match.isStarted || match.isFinished) return

        val cleanedMatch = removePlayerFromAllLists(match, player.id)
        val exists = match.expelledPlayers.any { it.id == player.id }

        val updatedMatch = cleanedMatch.copy(
            expelledPlayers = if (exists) cleanedMatch.expelledPlayers else cleanedMatch.expelledPlayers.toMutableList().apply { add(player) }
        )
        currentMatch = updatedMatch
        viewModelScope.launch { repository.saveMatchAndLineup(updatedMatch) }
    }

    fun toggleInjured(player: Player) {
        val match = currentMatch ?: return
        if (match.isStarted || match.isFinished) return

        val cleanedMatch = removePlayerFromAllLists(match, player.id)
        val exists = match.injuredPlayers.any { it.id == player.id }

        val updatedMatch = cleanedMatch.copy(
            injuredPlayers = if (exists) cleanedMatch.injuredPlayers else cleanedMatch.injuredPlayers.toMutableList().apply { add(player) }
        )
        currentMatch = updatedMatch
        viewModelScope.launch { repository.saveMatchAndLineup(updatedMatch) }
    }

    fun addStatEvent(playerId: Int? = null, playerName: String, type: String, count: Int) {
        val match = currentMatch ?: return
        if (count <= 0) return

        val newEvent = buildElapsedMatchEvent(
            match = match,
            type = type,
            playerName = playerName,
            playerId = playerId,
            count = count
        )

        val updatedMatch = match.copy(
            events = match.events.toMutableList().apply { add(newEvent) }
        )
        currentMatch = updatedMatch
        viewModelScope.launch { repository.addEvent(match.id, newEvent) }
    }

    fun clearSelectedFinishedMatch() {
        selectedFinishedMatch = null
        clearEditingFinishedMatch()
        selectedPlayerId = null
    }

    fun startMatch() {
        val match = currentMatch ?: return
        if (match.isStarted || match.isFinished) return
        clearEditingFinishedMatch()
        resetMatchControlState()

        val initializedTimes = initializePlayerTimesForMatch(match)

        val startedMatch = match.copy(
            isStarted = true,
            isFinished = false,
            playerTimes = initializedTimes
        )
        currentMatch = startedMatch

        viewModelScope.launch {
            repository.updateMatch(startedMatch)
            repository.savePlayerTimes(startedMatch)
        }

        matchTimerJob?.cancel()
        matchTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val updated = currentMatch ?: break
                if (!updated.isStarted || updated.isFinished) break

                if (isMatchPaused) {
                    continue
                }

                val newRemaining = (updated.remainingSeconds - 1).coerceAtLeast(0)

                if (newRemaining == 0) {
                    resetPauseUiState()
                    val finishedMatch = buildFinishedMatch(updated.copy(remainingSeconds = 0))

                    currentMatch = finishedMatch
                    putFinishedMatchLocally(finishedMatch)
                    shouldShowFinishedDialog = true

                    repository.saveMatchAndLineup(finishedMatch)
                    repository.replaceAllMatchEvents(finishedMatch.id, finishedMatch.events)
                    break
                } else {
                    val tickingMatch = updated.copy(remainingSeconds = newRemaining)
                    currentMatch = tickingMatch
                    repository.updateMatch(tickingMatch)
                }
            }
        }
    }

    fun getFormattedMatchTime(): String {
        val match = currentMatch ?: return "00:00"
        if (match.isFinished) return "Partido terminado"
        return formatMatchClock(match)
    }

    fun registerSwap(starter: Player, sub: Player) {
        val match = currentMatch ?: return
        if (!match.isStarted || match.isFinished) return

        val elapsed = elapsedSeconds(match)
        val minute = elapsed / 60
        val timestamp = getElapsedMatchTimeLabel()

        val currentTimes = match.playerTimes.toMutableMap()

        val starterTime = currentTimes[starter.id] ?: MatchPlayerTime(playerId = starter.id)
        val subTime = currentTimes[sub.id] ?: MatchPlayerTime(playerId = sub.id)

        currentTimes[starter.id] = starterTime.copy(
            accumulatedSeconds = starterTime.accumulatedSeconds +
                    (elapsed - (starterTime.lastEntrySecond ?: 0)).coerceAtLeast(0),
            isCurrentlyPlaying = false,
            lastEntrySecond = null
        )

        currentTimes[sub.id] = subTime.copy(
            accumulatedSeconds = subTime.accumulatedSeconds,
            isCurrentlyPlaying = true,
            lastEntrySecond = elapsed
        )

        val updatedStarters = match.starters.toMutableList().apply {
            val index = indexOfFirst { it.id == starter.id }
            if (index >= 0) this[index] = sub
        }

        val updatedSubstitutes = match.substitutes.toMutableList().apply {
            val index = indexOfFirst { it.id == sub.id }
            if (index >= 0) this[index] = starter
        }

        val swapEvent = MatchEvent(
            minute = minute,
            type = "Cambio",
            playerId = sub.id,
            playerName = sub.name,
            detail = "Entra ${sub.name} por ${starter.name}",
            timestampLabel = timestamp
        )

        val updatedMatch = match.copy(
            starters = updatedStarters.sortedBy { it.number }.toMutableList(),
            substitutes = updatedSubstitutes.sortedBy { it.number }.toMutableList(),
            playerTimes = currentTimes,
            events = match.events.toMutableList().apply { add(swapEvent) }
        )

        currentMatch = updatedMatch

        viewModelScope.launch {
            repository.saveMatchAndLineup(updatedMatch)
            repository.replaceAllMatchEvents(updatedMatch.id, updatedMatch.events)
        }
    }

    fun swapPlayerDuringMatch(starter: Player, substitute: Player) {
        val match = currentMatch ?: return
        if (!match.isStarted || match.isFinished) return

        val currentStarter = getCurrentStarters(match).firstOrNull { it.id == starter.id } ?: return
        val currentSub = getCurrentSubstitutes(match).firstOrNull { it.id == substitute.id } ?: return

        val elapsed = elapsedSeconds(match)
        val currentTimes = match.playerTimes.toMutableMap()

        val starterTime = currentTimes[currentStarter.id] ?: MatchPlayerTime(playerId = currentStarter.id)
        val subTime = currentTimes[currentSub.id] ?: MatchPlayerTime(playerId = currentSub.id)

        currentTimes[currentStarter.id] = starterTime.copy(
            accumulatedSeconds = starterTime.accumulatedSeconds +
                    (elapsed - (starterTime.lastEntrySecond ?: 0)).coerceAtLeast(0),
            isCurrentlyPlaying = false,
            lastEntrySecond = null
        )

        currentTimes[currentSub.id] = subTime.copy(
            isCurrentlyPlaying = true,
            lastEntrySecond = elapsed
        )

        val updatedMatch = match.copy(
            playerTimes = currentTimes
        )
        currentMatch = updatedMatch

        viewModelScope.launch {
            repository.savePlayerTimes(updatedMatch)
        }
    }

    fun getPlayerStats(playerId: Int): PlayerStats {
        val match = currentMatch ?: return PlayerStats()
        return match.statsByPlayerId.getOrPut(playerId) { PlayerStats() }
    }

    fun addCustomTeam(
        teamName: String,
        teamEmoji: String,
        players: List<TempPlayerInput>,
        logoUri: String? = null,
        shirtColorHex: String = "#1E6B45"
    ) {
        if (teamName.isBlank()) return
        if (players.size !in 5..30) return

        viewModelScope.launch {
            val persistentLogoUri = logoUri?.let { copyImageToInternalStorage(it) }

            val createdTeam = repository.addCustomTeam(
                teamName = teamName,
                teamEmoji = teamEmoji,
                players = players,
                logoUri = persistentLogoUri,
                shirtColorHex = shirtColorHex
            ) ?: return@launch

            selectedTeamId = createdTeam.id
        }
    }

    fun addEvent(minuteText: String, type: String, playerName: String) {
        val match = currentMatch ?: return
        val minute = minuteText.toIntOrNull() ?: 0
        val playerId = allPlayersOf(match).firstOrNull { it.name == playerName }?.id
        val event = MatchEvent(
            minute = minute,
            type = type,
            playerId = playerId,
            playerName = playerName
        )
        currentMatch = match.copy(events = match.events.toMutableList().apply { add(event) })
        viewModelScope.launch { repository.addEvent(match.id, event) }
    }

    private object ReportMetricKey {
        const val GOALS = "GOALS"
        const val GOALS_AGAINST = "GOALS_AGAINST"
        const val GOAL_PARTICIPATION = "GOAL_PARTICIPATION"
        const val GOAL_PARTICIPATION_AGAINST = "GOAL_PARTICIPATION_AGAINST"
        const val SHOTS_ON_TARGET_POSITIVE = "SHOTS_ON_TARGET_POSITIVE"
        const val SHOTS_ON_TARGET_NEGATIVE = "SHOTS_ON_TARGET_NEGATIVE"
        const val HALF_SHOTS_POSITIVE = "HALF_SHOTS_POSITIVE"
        const val HALF_SHOTS_NEGATIVE = "HALF_SHOTS_NEGATIVE"
        const val BALL_RECOVERED = "BALL_RECOVERED"
        const val BALL_LOST = "BALL_LOST"
        const val PASSES_GOOD = "PASSES_GOOD"
        const val PASSES_BAD = "PASSES_BAD"
        const val CROSSES_POSITIVE = "CROSSES_POSITIVE"
        const val CROSSES_NEGATIVE = "CROSSES_NEGATIVE"
        const val CLEARANCES_POSITIVE = "CLEARANCES_POSITIVE"
        const val CLEARANCES_NEGATIVE = "CLEARANCES_NEGATIVE"
        const val FOULS_FAVOR = "FOULS_FAVOR"
        const val FOULS_AGAINST = "FOULS_AGAINST"
        const val CORNERS_POSITIVE = "CORNERS_POSITIVE"
        const val CORNERS_NEGATIVE = "CORNERS_NEGATIVE"
        const val OFFSIDE_FAVOR = "OFFSIDE_FAVOR"
        const val OFFSIDE_AGAINST = "OFFSIDE_AGAINST"
        const val PENALTY_FAVOR = "PENALTY_FAVOR"
        const val PENALTY_AGAINST = "PENALTY_AGAINST"
        const val YELLOW_CARDS = "YELLOW_CARDS"
        const val RED_CARDS = "RED_CARDS"
        const val MINUTES_PLAYED = "MINUTES_PLAYED"
        const val STARTS = "STARTS"
        const val OPPONENT_GOAL_CHANCES = "OPPONENT_GOAL_CHANCES"
    }

    private data class PlayerHistoricalAccumulator(
        val player: Player,
        var matchesPlayed: Int = 0,
        var starts: Int = 0,
        var totalSecondsPlayed: Int = 0,
        val metricTotals: MutableMap<String, Int> = mutableMapOf()
    )

    private fun reportMetricDefinitions(): List<ReportMetricDefinition> {
        return listOf(
            ReportMetricDefinition(ReportMetricKey.GOALS, "Goles"),
            ReportMetricDefinition(ReportMetricKey.GOALS_AGAINST, "Goles en Contra"),
            ReportMetricDefinition(ReportMetricKey.GOAL_PARTICIPATION, "Participación Gol"),
            ReportMetricDefinition(ReportMetricKey.GOAL_PARTICIPATION_AGAINST, "Participación Gol Rival"),
            ReportMetricDefinition(ReportMetricKey.SHOTS_ON_TARGET_POSITIVE, "Tiro al Arco +"),
            ReportMetricDefinition(ReportMetricKey.SHOTS_ON_TARGET_NEGATIVE, "Tiro al Arco -"),
            ReportMetricDefinition(ReportMetricKey.HALF_SHOTS_POSITIVE, "Remate 1/2 +"),
            ReportMetricDefinition(ReportMetricKey.HALF_SHOTS_NEGATIVE, "Remate 1/2 -"),
            ReportMetricDefinition(ReportMetricKey.BALL_RECOVERED, "Balón Recuperado"),
            ReportMetricDefinition(ReportMetricKey.BALL_LOST, "Balón Perdido"),
            ReportMetricDefinition(ReportMetricKey.PASSES_GOOD, "Pases Buenos"),
            ReportMetricDefinition(ReportMetricKey.PASSES_BAD, "Pases Malos"),
            ReportMetricDefinition(ReportMetricKey.CROSSES_POSITIVE, "Centros +"),
            ReportMetricDefinition(ReportMetricKey.CROSSES_NEGATIVE, "Centros -"),
            ReportMetricDefinition(ReportMetricKey.CLEARANCES_POSITIVE, "Rechazos +"),
            ReportMetricDefinition(ReportMetricKey.CLEARANCES_NEGATIVE, "Rechazos -"),
            ReportMetricDefinition(ReportMetricKey.FOULS_FAVOR, "Falta a Favor"),
            ReportMetricDefinition(ReportMetricKey.FOULS_AGAINST, "Falta en Contra"),
            ReportMetricDefinition(ReportMetricKey.CORNERS_POSITIVE, "Corner +"),
            ReportMetricDefinition(ReportMetricKey.CORNERS_NEGATIVE, "Corner -"),
            ReportMetricDefinition(ReportMetricKey.OFFSIDE_FAVOR, "Off Side a Favor"),
            ReportMetricDefinition(ReportMetricKey.OFFSIDE_AGAINST, "Off Side en Contra"),
            ReportMetricDefinition(ReportMetricKey.PENALTY_FAVOR, "Penal a Favor"),
            ReportMetricDefinition(ReportMetricKey.PENALTY_AGAINST, "Penal en Contra"),
            ReportMetricDefinition(ReportMetricKey.YELLOW_CARDS, "Amarillas"),
            ReportMetricDefinition(ReportMetricKey.RED_CARDS, "Rojas"),
            ReportMetricDefinition(
                key = ReportMetricKey.MINUTES_PLAYED,
                label = "Minutos Jugados",
                kind = ReportMetricKind.MINUTES,
                supportsTimeline = false
            ),
            ReportMetricDefinition(
                key = ReportMetricKey.STARTS,
                label = "Titularidades",
                kind = ReportMetricKind.STARTS,
                supportsTimeline = false
            ),
            ReportMetricDefinition(
                key = ReportMetricKey.OPPONENT_GOAL_CHANCES,
                label = "Oportunidad de Gol Rival",
                supportsPlayerBreakdown = false
            )
        )
    }

    private fun parseEventCountFromDetail(detail: String): Int {
        return detail.substringAfter(": ", "")
            .toIntOrNull()
            ?.coerceAtLeast(1)
            ?: 1
    }

    private fun metricDefinitionByKey(key: String): ReportMetricDefinition? {
        return reportMetricDefinitions().firstOrNull { it.key == key }
    }

    private fun metricKeyFromEvent(type: String, match: MatchRecord): String? {
        return when {
            isTeamGoalType(type, match) -> ReportMetricKey.GOALS
            isOpponentGoalType(type, match) -> ReportMetricKey.GOALS_AGAINST
            isTeamAssistType(type, match) -> ReportMetricKey.GOAL_PARTICIPATION
            isOpponentAssistType(type, match) -> ReportMetricKey.GOAL_PARTICIPATION_AGAINST
            type == "Tiro al Arco +" -> ReportMetricKey.SHOTS_ON_TARGET_POSITIVE
            type == "Tiro al Arco -" -> ReportMetricKey.SHOTS_ON_TARGET_NEGATIVE
            type == "Remate 1/2 +" -> ReportMetricKey.HALF_SHOTS_POSITIVE
            type == "Remate 1/2 -" -> ReportMetricKey.HALF_SHOTS_NEGATIVE
            type == "Balón Recogido a Favor" || type == "Balón Recup." -> ReportMetricKey.BALL_RECOVERED
            type == "Balón Recogido en Contra" || type == "Balón Perdido" -> ReportMetricKey.BALL_LOST
            type == "Pases Buenos" -> ReportMetricKey.PASSES_GOOD
            type == "Pases Malos" -> ReportMetricKey.PASSES_BAD
            type == "Centros +" -> ReportMetricKey.CROSSES_POSITIVE
            type == "Centros -" -> ReportMetricKey.CROSSES_NEGATIVE
            type == "Rechazos +" -> ReportMetricKey.CLEARANCES_POSITIVE
            type == "Rechazos -" -> ReportMetricKey.CLEARANCES_NEGATIVE
            isTeamFoulType(type, match) -> ReportMetricKey.FOULS_FAVOR
            isOpponentFoulType(type, match) -> ReportMetricKey.FOULS_AGAINST
            type == "Corner +" -> ReportMetricKey.CORNERS_POSITIVE
            type == "Corner -" -> ReportMetricKey.CORNERS_NEGATIVE
            isTeamOffsideType(type, match) -> ReportMetricKey.OFFSIDE_FAVOR
            isOpponentOffsideType(type, match) -> ReportMetricKey.OFFSIDE_AGAINST
            isTeamPenaltyType(type, match) -> ReportMetricKey.PENALTY_FAVOR
            isOpponentPenaltyType(type, match) -> ReportMetricKey.PENALTY_AGAINST
            type == "Amarilla" -> ReportMetricKey.YELLOW_CARDS
            type == "Roja" -> ReportMetricKey.RED_CARDS
            isOpponentGoalChanceType(type, match) -> ReportMetricKey.OPPONENT_GOAL_CHANCES
            else -> null
        }
    }

    private fun reportMatchesForSelectedTeam(): List<MatchRecord> {
        val teamId = selectedTeam?.id ?: return emptyList()
        return finishedMatches
            .filter { it.teamId == teamId && it.isFinished }
            .sortedByDescending { it.finishedAtMillis ?: it.createdAtMillis }
    }

    private fun reportPlayersForMatches(matches: List<MatchRecord>): List<Player> {
        val currentPlayers = selectedTeam?.players.orEmpty()
        return (currentPlayers + matches.flatMap { allPlayersOf(it) })
            .distinctBy { it.id }
            .sortedWith(compareBy<Player> { it.number }.thenBy { it.name })
    }

    private fun buildHistoricalAccumulators(matches: List<MatchRecord>): Map<Int, PlayerHistoricalAccumulator> {
        val players = reportPlayersForMatches(matches)
        val accumulators = players.associate { player ->
            player.id to PlayerHistoricalAccumulator(player = player)
        }.toMutableMap()

        matches.forEach { match ->
            val matchPlayers = allPlayersOf(match).distinctBy { it.id }
            matchPlayers.forEach { player ->
                val accumulator = accumulators.getOrPut(player.id) {
                    PlayerHistoricalAccumulator(player = player)
                }
                accumulator.matchesPlayed += 1
                if (match.starters.any { it.id == player.id }) {
                    accumulator.starts += 1
                }
                accumulator.totalSecondsPlayed += match.playerTimes[player.id]?.accumulatedSeconds ?: 0
            }

            match.events.forEach { event ->
                val metricKey = metricKeyFromEvent(event.type, match) ?: return@forEach
                val playerId = event.playerId ?: return@forEach
                val accumulator = accumulators[playerId] ?: return@forEach
                val count = parseEventCountFromDetail(event.detail)
                accumulator.metricTotals[metricKey] = (accumulator.metricTotals[metricKey] ?: 0) + count
            }
        }

        return accumulators
    }

    private fun valueForMetric(
        metric: ReportMetricDefinition,
        accumulator: PlayerHistoricalAccumulator
    ): Double {
        return when (metric.kind) {
            ReportMetricKind.EVENT_COUNT -> (accumulator.metricTotals[metric.key] ?: 0).toDouble()
            ReportMetricKind.MINUTES -> accumulator.totalSecondsPlayed / 60.0
            ReportMetricKind.STARTS -> accumulator.starts.toDouble()
        }
    }

    private fun buildRankingRows(
        metricKey: String,
        includeZeroValues: Boolean
    ): List<ReportRankingRow> {
        val metric = metricDefinitionByKey(metricKey) ?: return emptyList()
        val accumulators = buildHistoricalAccumulators(reportMatchesForSelectedTeam())

        val baseRows = accumulators.values
            .map { accumulator ->
                val total = valueForMetric(metric, accumulator)
                val average = if (accumulator.matchesPlayed > 0) {
                    total / accumulator.matchesPlayed
                } else {
                    0.0
                }
                Triple(accumulator, total, average)
            }
            .filter { (_, total, _) -> includeZeroValues || total > 0.0 }
            .sortedWith(
                compareByDescending<Triple<PlayerHistoricalAccumulator, Double, Double>> { it.second }
                    .thenByDescending { it.third }
                    .thenBy { it.first.player.name.lowercase() }
                    .thenBy { it.first.player.number }
            )

        val rows = mutableListOf<ReportRankingRow>()
        var densePosition = 0
        var lastTotal: Double? = null
        var lastAverage: Double? = null

        baseRows.forEachIndexed { index, (accumulator, total, average) ->
            val sameAsPrevious = lastTotal != null && lastAverage != null &&
                    abs(total - lastTotal!!) < 0.0001 &&
                    abs(average - lastAverage!!) < 0.0001

            if (!sameAsPrevious) {
                densePosition = if (index == 0) 1 else densePosition + 1
                lastTotal = total
                lastAverage = average
            }

            rows.add(
                ReportRankingRow(
                    position = densePosition,
                    playerId = accumulator.player.id,
                    playerName = accumulator.player.name,
                    shirtNumber = accumulator.player.number,
                    total = total,
                    average = average,
                    matchesPlayed = accumulator.matchesPlayed
                )
            )
        }

        return rows
    }

    fun getReportRankingMetrics(): List<ReportMetricDefinition> {
        return reportMetricDefinitions().filter { it.supportsPlayerBreakdown }
    }

    fun getReportTimelineMetrics(): List<ReportMetricDefinition> {
        return reportMetricDefinitions().filter { it.supportsTimeline }
    }

    fun getReportPlayers(): List<Player> {
        return reportPlayersForMatches(reportMatchesForSelectedTeam())
    }

    fun getReportMatchesForSelectedTeam(): List<MatchRecord> {
        return reportMatchesForSelectedTeam()
    }

    fun buildReportRanking(metricKey: String): List<ReportRankingRow> {
        return buildRankingRows(metricKey = metricKey, includeZeroValues = false)
    }

    fun buildReportTimeBreakdown(metricKey: String, matchId: Int?): ReportTimeBreakdown? {
        val metric = metricDefinitionByKey(metricKey)?.takeIf { it.supportsTimeline } ?: return null
        val matches = reportMatchesForSelectedTeam().let { finished ->
            if (matchId == null) finished else finished.filter { it.id == matchId }
        }
        if (matches.isEmpty()) return null

        var firstQuarter = 0
        var secondQuarter = 0
        var thirdQuarter = 0
        var lastQuarter = 0

        matches.forEach { match ->
            match.events.forEach { event ->
                if (metricKeyFromEvent(event.type, match) != metricKey) return@forEach
                val count = parseEventCountFromDetail(event.detail)
                when (event.minute) {
                    in 0..20 -> firstQuarter += count
                    in 21..40 -> secondQuarter += count
                    in 41..60 -> thirdQuarter += count
                    else -> lastQuarter += count
                }
            }
        }

        val blocks = listOf(
            ReportTimeBlock("Primer Cuarto (0-20)", firstQuarter),
            ReportTimeBlock("Segundo Cuarto (21-40)", secondQuarter),
            ReportTimeBlock("Tercer Cuarto (41-60)", thirdQuarter),
            ReportTimeBlock("Último Cuarto (61+)", lastQuarter)
        )

        val matchLabel = if (matchId == null) {
            "Todos los partidos"
        } else {
            matches.firstOrNull()?.let { selected ->
                val rival = selected.rivalName.ifBlank { "Sin rival" }
                "${selected.teamName} vs $rival"
            } ?: "Partido"
        }

        return ReportTimeBreakdown(
            metric = metric,
            matchLabel = matchLabel,
            total = blocks.sumOf { it.total },
            blocks = blocks
        )
    }

    fun buildPlayerReport(playerId: Int): ReportPlayerSummary? {
        val matches = reportMatchesForSelectedTeam()
        if (matches.isEmpty()) return null

        val player = getReportPlayers().firstOrNull { it.id == playerId } ?: return null
        val accumulators = buildHistoricalAccumulators(matches)
        val accumulator = accumulators[playerId] ?: PlayerHistoricalAccumulator(player = player)

        val metricDefinitions = getReportRankingMetrics()
        val rankingMaps = metricDefinitions.associate { definition ->
            definition.key to buildRankingRows(definition.key, includeZeroValues = true)
                .associateBy { it.playerId }
        }

        val statRows = metricDefinitions.map { metric ->
            val total = valueForMetric(metric, accumulator)
            val average = if (accumulator.matchesPlayed > 0) {
                total / accumulator.matchesPlayed
            } else {
                0.0
            }
            ReportPlayerStatRow(
                metric = metric,
                total = total,
                average = average,
                position = rankingMaps[metric.key]?.get(playerId)?.position
            )
        }

        return ReportPlayerSummary(
            playerId = player.id,
            playerName = player.name,
            shirtNumber = player.number,
            matchesPlayed = accumulator.matchesPlayed,
            teamMatches = matches.size,
            totalMinutes = accumulator.totalSecondsPlayed / 60.0,
            averageMinutes = if (accumulator.matchesPlayed > 0) {
                (accumulator.totalSecondsPlayed / 60.0) / accumulator.matchesPlayed
            } else {
                0.0
            },
            starts = accumulator.starts,
            stats = statRows
        )
    }

}
