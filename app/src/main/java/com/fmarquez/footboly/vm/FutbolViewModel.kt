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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class FutbolViewModel(application: Application) : AndroidViewModel(application) {

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

    private fun buildDraftFromMatch(match: MatchRecord, playerId: Int): PlayerStatsDraft {
        val allPlayers = match.starters + match.substitutes + match.expelledPlayers + match.injuredPlayers
        val player = allPlayers.firstOrNull { it.id == playerId }
            ?: return PlayerStatsDraft(playerId = playerId, matchId = match.id)

        val playerEvents = match.events.filter { it.playerId == player.id }

        var draft = PlayerStatsDraft(playerId = playerId, matchId = match.id)

        playerEvents.forEach { event ->
            val count = parseEventCount(event)
            draft = when (event.type) {
                "Gol a Favor" -> draft.copy(golFavor = draft.golFavor + count)
                "Gol en Contra" -> draft.copy(golContra = draft.golContra + count)
                "Tiro al Arco +" -> draft.copy(tiroAlArcoPositivo = draft.tiroAlArcoPositivo + count)
                "Tiro al Arco -" -> draft.copy(tiroAlArcoNegativo = draft.tiroAlArcoNegativo + count)
                "Participación de Gol a Favor" -> draft.copy(participacionGolFavor = draft.participacionGolFavor + count)
                "Participación de Gol en Contra" -> draft.copy(participacionGolContra = draft.participacionGolContra + count)
                "Remate 1/2 +" -> draft.copy(remate12Positivo = draft.remate12Positivo + count)
                "Remate 1/2 -" -> draft.copy(remate12Negativo = draft.remate12Negativo + count)

                "Balón Recogido a Favor" -> draft.copy(balonRecogidoFavor = draft.balonRecogidoFavor + count)
                "Balón Recogido en Contra" -> draft.copy(balonRecogidoContra = draft.balonRecogidoContra + count)
                "Pases Buenos" -> draft.copy(pasesBuenos = draft.pasesBuenos + count)
                "Pases Malos" -> draft.copy(pasesMalos = draft.pasesMalos + count)
                "Centros +" -> draft.copy(centrosPositivos = draft.centrosPositivos + count)
                "Centros -" -> draft.copy(centrosNegativos = draft.centrosNegativos + count)
                "Rechazos +" -> draft.copy(rechazosPositivos = draft.rechazosPositivos + count)
                "Rechazos -" -> draft.copy(rechazosNegativos = draft.rechazosNegativos + count)

                "Falta a Favor" -> draft.copy(faltaFavor = draft.faltaFavor + count)
                "Falta en Contra" -> draft.copy(faltaContra = draft.faltaContra + count)
                "Corner +" -> draft.copy(cornerPositivo = draft.cornerPositivo + count)
                "Corner -" -> draft.copy(cornerNegativo = draft.cornerNegativo + count)
                "Tiro Libre a Favor" -> draft.copy(tiroLibreFavor = draft.tiroLibreFavor + count)
                "Tiro Libre en Contra" -> draft.copy(tiroLibreContra = draft.tiroLibreContra + count)
                "Penal a Favor" -> draft.copy(penalFavor = draft.penalFavor + count)
                "Penal en Contra" -> draft.copy(penalContra = draft.penalContra + count)

                "Amarilla" -> draft.copy(amarilla = draft.amarilla + count)
                "Roja" -> draft.copy(roja = draft.roja + count)
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
        player: Player,
        draft: PlayerStatsDraft,
        oldEventsOfPlayer: List<MatchEvent>,
        defaultTimestamp: String
    ): List<MatchEvent> {
        val result = mutableListOf<MatchEvent>()

        fun oldEvent(type: String) = oldEventsOfPlayer.firstOrNull { it.type == type }

        fun addIfNeeded(type: String, count: Int) {
            if (count <= 0) return
            val previous = oldEvent(type)
            result.add(
                createEventFromDraft(
                    player = player,
                    type = type,
                    count = count,
                    timestampLabel = previous?.timestampLabel ?: defaultTimestamp,
                    minute = previous?.minute ?: 0
                )
            )
        }

        addIfNeeded("Gol a Favor", draft.golFavor)
        addIfNeeded("Gol en Contra", draft.golContra)
        addIfNeeded("Tiro al Arco +", draft.tiroAlArcoPositivo)
        addIfNeeded("Tiro al Arco -", draft.tiroAlArcoNegativo)
        addIfNeeded("Participación de Gol a Favor", draft.participacionGolFavor)
        addIfNeeded("Participación de Gol en Contra", draft.participacionGolContra)
        addIfNeeded("Remate 1/2 +", draft.remate12Positivo)
        addIfNeeded("Remate 1/2 -", draft.remate12Negativo)

        addIfNeeded("Balón Recogido a Favor", draft.balonRecogidoFavor)
        addIfNeeded("Balón Recogido en Contra", draft.balonRecogidoContra)
        addIfNeeded("Pases Buenos", draft.pasesBuenos)
        addIfNeeded("Pases Malos", draft.pasesMalos)
        addIfNeeded("Centros +", draft.centrosPositivos)
        addIfNeeded("Centros -", draft.centrosNegativos)
        addIfNeeded("Rechazos +", draft.rechazosPositivos)
        addIfNeeded("Rechazos -", draft.rechazosNegativos)

        addIfNeeded("Falta a Favor", draft.faltaFavor)
        addIfNeeded("Falta en Contra", draft.faltaContra)
        addIfNeeded("Corner +", draft.cornerPositivo)
        addIfNeeded("Corner -", draft.cornerNegativo)
        addIfNeeded("Tiro Libre a Favor", draft.tiroLibreFavor)
        addIfNeeded("Tiro Libre en Contra", draft.tiroLibreContra)
        addIfNeeded("Penal a Favor", draft.penalFavor)
        addIfNeeded("Penal en Contra", draft.penalContra)

        addIfNeeded("Amarilla", draft.amarilla)
        addIfNeeded("Roja", draft.roja)

        return result
    }

    private fun buildEditChanges(
        playerName: String,
        original: PlayerStatsDraft,
        updated: PlayerStatsDraft
    ): List<String> {
        val changes = mutableListOf<String>()

        fun compare(label: String, oldValue: Int, newValue: Int) {
            if (oldValue != newValue) changes.add("$playerName · $label: $oldValue → $newValue")
        }

        compare("Gol a Favor", original.golFavor, updated.golFavor)
        compare("Gol en Contra", original.golContra, updated.golContra)
        compare("Tiro al Arco +", original.tiroAlArcoPositivo, updated.tiroAlArcoPositivo)
        compare("Tiro al Arco -", original.tiroAlArcoNegativo, updated.tiroAlArcoNegativo)
        compare("Participación de Gol a Favor", original.participacionGolFavor, updated.participacionGolFavor)
        compare("Participación de Gol en Contra", original.participacionGolContra, updated.participacionGolContra)
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

        compare("Falta a Favor", original.faltaFavor, updated.faltaFavor)
        compare("Falta en Contra", original.faltaContra, updated.faltaContra)
        compare("Corner +", original.cornerPositivo, updated.cornerPositivo)
        compare("Corner -", original.cornerNegativo, updated.cornerNegativo)
        compare("Tiro Libre a Favor", original.tiroLibreFavor, updated.tiroLibreFavor)
        compare("Tiro Libre en Contra", original.tiroLibreContra, updated.tiroLibreContra)
        compare("Penal a Favor", original.penalFavor, updated.penalFavor)
        compare("Penal en Contra", original.penalContra, updated.penalContra)

        compare("Amarilla", original.amarilla, updated.amarilla)
        compare("Roja", original.roja, updated.roja)

        return changes
    }

    private fun saveEditedFinishedMatch(playerId: Int): List<String> {
        val match = editingFinishedMatch ?: return emptyList()
        val allPlayers = match.starters + match.substitutes + match.expelledPlayers + match.injuredPlayers
        val player = allPlayers.firstOrNull { it.id == playerId } ?: return emptyList()

        val key = statsDraftKey(match.id, playerId)
        val updatedDraft = playerStatsDrafts[key] ?: return emptyList()
        val originalDraft = originalPlayerStatsDrafts[key] ?: buildDraftFromMatch(match, playerId)

        val changes = buildEditChanges(player.name, originalDraft, updatedDraft)

        val oldEventsOfPlayer = match.events.filter { it.playerId == playerId }
        val eventsWithoutPlayer = match.events.filterNot { it.playerId == playerId }.toMutableList()

        val rebuiltEventsForPlayer = buildEventsFromDraftForPlayer(
            player = player,
            draft = updatedDraft,
            oldEventsOfPlayer = oldEventsOfPlayer,
            defaultTimestamp = match.finishedAtLabel.ifBlank { "00:00" }
        )

        val updatedMatch = match.copy(
            events = (eventsWithoutPlayer + rebuiltEventsForPlayer).toMutableList()
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
        val emptyDraft = PlayerStatsDraft(
            playerId = playerId,
            matchId = match.id
        )
        updatePlayerStatsDraft(emptyDraft)
    }

    fun savePlayerStatsDraftAsEvents(playerId: Int): List<String> {
        val match = getActiveMatchForStats() ?: return emptyList()
        val draft = getOrCreatePlayerStatsDraft(playerId)
        val player = (match.starters + match.substitutes + match.expelledPlayers + match.injuredPlayers)
            .distinctBy { it.id }
            .firstOrNull { it.id == playerId }
            ?: return emptyList()

        val minute = getCurrentMatchMinute(match)
        val timestamp = formatMatchClock(match)

        val newEvents = mutableListOf<MatchEvent>()
        val changes = mutableListOf<String>()

        fun addEvent(type: String, count: Int) {
            if (count <= 0) return

            newEvents.add(
                MatchEvent(
                    minute = minute,
                    type = type,
                    playerId = player.id,
                    playerName = player.name,
                    detail = "Cantidad: $count",
                    timestampLabel = timestamp
                )
            )
            changes.add("${player.name}: $type x$count")
        }

        // Bloque 1
        addEvent("Gol a Favor", draft.golFavor)
        addEvent("Gol en Contra", draft.golContra)
        addEvent("Tiro al Arco +", draft.tiroAlArcoPositivo)
        addEvent("Tiro al Arco -", draft.tiroAlArcoNegativo)
        addEvent("Participación de Gol a Favor", draft.participacionGolFavor)
        addEvent("Participación de Gol en Contra", draft.participacionGolContra)
        addEvent("Remate 1/2 +", draft.remate12Positivo)
        addEvent("Remate 1/2 -", draft.remate12Negativo)

        // Bloque 2
        addEvent("Balón Recogido a Favor", draft.balonRecogidoFavor)
        addEvent("Balón Recogido en Contra", draft.balonRecogidoContra)
        addEvent("Pases Buenos", draft.pasesBuenos)
        addEvent("Pases Malos", draft.pasesMalos)
        addEvent("Centros +", draft.centrosPositivos)
        addEvent("Centros -", draft.centrosNegativos)
        addEvent("Rechazos +", draft.rechazosPositivos)
        addEvent("Rechazos -", draft.rechazosNegativos)

        // Bloque 3
        addEvent("Falta a Favor", draft.faltaFavor)
        addEvent("Falta en Contra", draft.faltaContra)
        addEvent("Corner +", draft.cornerPositivo)
        addEvent("Corner -", draft.cornerNegativo)
        addEvent("Tiro Libre a Favor", draft.tiroLibreFavor)
        addEvent("Tiro Libre en Contra", draft.tiroLibreContra)
        addEvent("Penal a Favor", draft.penalFavor)
        addEvent("Penal en Contra", draft.penalContra)

        // Se mantienen
        addEvent("Amarilla", draft.amarilla)
        addEvent("Roja", draft.roja)

        if (newEvents.isEmpty()) return emptyList()

        if (isEditingFinishedMatchMode()) {
            return saveEditedFinishedMatch(playerId)
        } else {
            val updatedMatch = match.copy(
                events = (match.events + newEvents).sortedBy { it.minute }.toMutableList()
            )
            currentMatch = updatedMatch

            viewModelScope.launch {
                newEvents.forEach { event ->
                    repository.addEvent(updatedMatch.id, event)
                }
            }
        }

        clearPlayerStatsDraft(playerId)
        return changes
    }

    fun updatePlayerStatsDraft(updatedDraft: PlayerStatsDraft) {
        val key = statsDraftKey(updatedDraft.matchId, updatedDraft.playerId)
        playerStatsDrafts[key] = updatedDraft
    }

    fun setMatchDuration(minutes: Int) {
        val match = currentMatch ?: return
        val safeMinutes = minutes.coerceIn(10, 90)
        val updatedMatch = match.copy(
            totalSeconds = safeMinutes * 60,
            remainingSeconds = safeMinutes * 60
        )
        currentMatch = updatedMatch
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
        val updatedMatch = match.copy(
            opponentGoals = (match.opponentGoals + delta).coerceAtLeast(0)
        )
        currentMatch = updatedMatch
        viewModelScope.launch { repository.updateMatch(updatedMatch) }
    }

    fun updateOpponentGoalChances(delta: Int) {
        val match = currentMatch ?: return
        val updatedMatch = match.copy(
            opponentGoalChances = (match.opponentGoalChances + delta).coerceAtLeast(0)
        )
        currentMatch = updatedMatch
        viewModelScope.launch { repository.updateMatch(updatedMatch) }
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
        viewModelScope.launch { repository.removePlayer(team.id, player.id) }
    }

    fun createNewMatch(onCreated: (() -> Unit)? = null) {
        val team = selectedTeam ?: return
        clearEditingFinishedMatch()
        selectedFinishedMatch = null
        selectedPlayerId = null
        matchTimerJob?.cancel()
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
        val allPlayers = match.starters + match.substitutes + match.expelledPlayers + match.injuredPlayers
        return allPlayers.firstOrNull { it.id == id }
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

        val finalizedPlayerTimes = finalizePlayingTimes(match)

        val finishedMatch = match.copy(
            playerTimes = finalizedPlayerTimes,
            isFinished = true,
            remainingSeconds = 0,
            finishedAtMillis = System.currentTimeMillis(),
            finishedAtLabel = getElapsedMatchTimeLabel(match)
        )

        currentMatch = finishedMatch
        putFinishedMatchLocally(finishedMatch)
        shouldShowFinishedDialog = true

        viewModelScope.launch {
            repository.updateMatch(finishedMatch)
            repository.savePlayerTimes(finishedMatch)
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
        val elapsed = elapsedSeconds(match)
        val minute = elapsed / 60
        val timestamp = getElapsedMatchTimeLabel()
        val newEvent = MatchEvent(
            minute = minute,
            type = type,
            playerId = playerId,
            playerName = playerName,
            detail = "$type: $count",
            timestampLabel = timestamp
        )
        val updatedMatch = match.copy(events = match.events.toMutableList().apply { add(newEvent) })
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

                val newRemaining = (updated.remainingSeconds - 1).coerceAtLeast(0)

                if (newRemaining == 0) {
                    val zeroMatch = updated.copy(remainingSeconds = 0)
                    val finalizedPlayerTimes = finalizePlayingTimes(zeroMatch)

                    val finishedMatch = zeroMatch.copy(
                        playerTimes = finalizedPlayerTimes,
                        isFinished = true,
                        finishedAtMillis = System.currentTimeMillis(),
                        finishedAtLabel = getElapsedMatchTimeLabel(zeroMatch)
                    )

                    currentMatch = finishedMatch
                    putFinishedMatchLocally(finishedMatch)
                    shouldShowFinishedDialog = true

                    repository.updateMatch(finishedMatch)
                    repository.savePlayerTimes(finishedMatch)
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
        val minutes = match.remainingSeconds / 60
        val seconds = match.remainingSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun registerSwap(starter: Player, sub: Player) {
        val match = currentMatch ?: return
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
            isCurrentlyPlaying = true,
            lastEntrySecond = elapsed
        )

        val swapEvent = MatchEvent(
            minute = minute,
            type = "Cambio",
            playerId = sub.id,
            playerName = sub.name,
            detail = "Entra ${sub.name} por ${starter.name}",
            timestampLabel = timestamp
        )

        val updatedMatch = match.copy(
            playerTimes = currentTimes,
            events = match.events.toMutableList().apply { add(swapEvent) }
        )

        currentMatch = updatedMatch

        viewModelScope.launch {
            repository.addEvent(updatedMatch.id, swapEvent)
            repository.savePlayerTimes(updatedMatch)
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
        val allPlayers = match.starters + match.substitutes + match.expelledPlayers + match.injuredPlayers
        val playerId = allPlayers.firstOrNull { it.name == playerName }?.id
        val event = MatchEvent(
            minute = minute,
            type = type,
            playerId = playerId,
            playerName = playerName
        )
        currentMatch = match.copy(events = match.events.toMutableList().apply { add(event) })
        viewModelScope.launch { repository.addEvent(match.id, event) }
    }
}