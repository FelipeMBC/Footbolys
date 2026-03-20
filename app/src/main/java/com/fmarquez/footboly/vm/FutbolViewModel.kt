package com.fmarquez.footboly.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fmarquez.footboly.data.local.db.FootbolyDatabase
import com.fmarquez.footboly.data.repository.FootballRepository
import com.fmarquez.footboly.modelos.MatchEvent
import com.fmarquez.footboly.modelos.MatchRecord
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.modelos.PlayerStats
import com.fmarquez.footboly.modelos.PlayerStatsDraft
import com.fmarquez.footboly.modelos.Team
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        val player = (match.starters + match.substitutes).firstOrNull { it.id == playerId }
            ?: return PlayerStatsDraft(playerId = playerId, matchId = match.id)

        val playerEvents = match.events.filter { it.playerId == player.id }

        var draft = PlayerStatsDraft(
            playerId = playerId,
            matchId = match.id
        )

        playerEvents.forEach { event ->
            val count = parseEventCount(event)

            draft = when (event.type) {
                "Gol" -> draft.copy(gol = draft.gol + count)
                "Asistencia" -> draft.copy(asistencia = draft.asistencia + count)
                "Amarilla" -> draft.copy(amarilla = draft.amarilla + count)
                "Roja" -> draft.copy(roja = draft.roja + count)
                "Disparos al Arco" -> draft.copy(disparosAlArco = draft.disparosAlArco + count)
                "Ocasiones de Gol" -> draft.copy(ocasionesDeGol = draft.ocasionesDeGol + count)
                "Pelotas Perdidas" -> draft.copy(pelotasPerdidas = draft.pelotasPerdidas + count)
                "Pelotas Recuperadas" -> draft.copy(pelotasRecuperadas = draft.pelotasRecuperadas + count)
                "Centros Buenos" -> draft.copy(centrosBuenos = draft.centrosBuenos + count)
                "Centros Malos" -> draft.copy(centrosMalos = draft.centrosMalos + count)
                "Falta a Favor" -> draft.copy(faltaAFavor = draft.faltaAFavor + count)
                "Falta en Contra" -> draft.copy(faltaEnContra = draft.faltaEnContra + count)
                "Corner a Favor" -> draft.copy(cornerAFavor = draft.cornerAFavor + count)
                "Corner en Contra" -> draft.copy(cornerEnContra = draft.cornerEnContra + count)
                "Tiro Libre a Favor" -> draft.copy(tiroLibreAFavor = draft.tiroLibreAFavor + count)
                "Tiro Libre en Contra" -> draft.copy(tiroLibreEnContra = draft.tiroLibreEnContra + count)
                "Tiro Libre Lateral a Favor" -> draft.copy(tiroLibreLateralAFavor = draft.tiroLibreLateralAFavor + count)
                "Tiro Libre Lateral en Contra" -> draft.copy(tiroLibreLateralEnContra = draft.tiroLibreLateralEnContra + count)
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
                PlayerStatsDraft(
                    playerId = playerId,
                    matchId = match.id
                )
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

        fun oldEvent(type: String): MatchEvent? {
            return oldEventsOfPlayer.firstOrNull { it.type == type }
        }

        fun addIfNeeded(type: String, count: Int) {
            if (count <= 0) return

            val previous = oldEvent(type)
            val minute = previous?.minute ?: 0
            val timestamp = previous?.timestampLabel ?: defaultTimestamp

            result.add(
                createEventFromDraft(
                    player = player,
                    type = type,
                    count = count,
                    timestampLabel = timestamp,
                    minute = minute
                )
            )
        }

        addIfNeeded("Gol", draft.gol)
        addIfNeeded("Asistencia", draft.asistencia)
        addIfNeeded("Amarilla", draft.amarilla)
        addIfNeeded("Roja", draft.roja)
        addIfNeeded("Disparos al Arco", draft.disparosAlArco)
        addIfNeeded("Ocasiones de Gol", draft.ocasionesDeGol)
        addIfNeeded("Pelotas Perdidas", draft.pelotasPerdidas)
        addIfNeeded("Pelotas Recuperadas", draft.pelotasRecuperadas)
        addIfNeeded("Centros Buenos", draft.centrosBuenos)
        addIfNeeded("Centros Malos", draft.centrosMalos)
        addIfNeeded("Falta a Favor", draft.faltaAFavor)
        addIfNeeded("Falta en Contra", draft.faltaEnContra)
        addIfNeeded("Corner a Favor", draft.cornerAFavor)
        addIfNeeded("Corner en Contra", draft.cornerEnContra)
        addIfNeeded("Tiro Libre a Favor", draft.tiroLibreAFavor)
        addIfNeeded("Tiro Libre en Contra", draft.tiroLibreEnContra)
        addIfNeeded("Tiro Libre Lateral a Favor", draft.tiroLibreLateralAFavor)
        addIfNeeded("Tiro Libre Lateral en Contra", draft.tiroLibreLateralEnContra)

        return result
    }

    private fun buildEditChanges(
        playerName: String,
        original: PlayerStatsDraft,
        updated: PlayerStatsDraft
    ): List<String> {
        val changes = mutableListOf<String>()

        fun compare(label: String, oldValue: Int, newValue: Int) {
            if (oldValue != newValue) {
                changes.add("$playerName · $label: $oldValue → $newValue")
            }
        }

        compare("Gol", original.gol, updated.gol)
        compare("Asistencia", original.asistencia, updated.asistencia)
        compare("Amarilla", original.amarilla, updated.amarilla)
        compare("Roja", original.roja, updated.roja)
        compare("Disparos al Arco", original.disparosAlArco, updated.disparosAlArco)
        compare("Ocasiones de Gol", original.ocasionesDeGol, updated.ocasionesDeGol)
        compare("Pelotas Perdidas", original.pelotasPerdidas, updated.pelotasPerdidas)
        compare("Pelotas Recuperadas", original.pelotasRecuperadas, updated.pelotasRecuperadas)
        compare("Centros Buenos", original.centrosBuenos, updated.centrosBuenos)
        compare("Centros Malos", original.centrosMalos, updated.centrosMalos)
        compare("Falta a Favor", original.faltaAFavor, updated.faltaAFavor)
        compare("Falta en Contra", original.faltaEnContra, updated.faltaEnContra)
        compare("Corner a Favor", original.cornerAFavor, updated.cornerAFavor)
        compare("Corner en Contra", original.cornerEnContra, updated.cornerEnContra)
        compare("Tiro Libre a Favor", original.tiroLibreAFavor, updated.tiroLibreAFavor)
        compare("Tiro Libre en Contra", original.tiroLibreEnContra, updated.tiroLibreEnContra)
        compare("Tiro Libre Lateral a Favor", original.tiroLibreLateralAFavor, updated.tiroLibreLateralAFavor)
        compare("Tiro Libre Lateral en Contra", original.tiroLibreLateralEnContra, updated.tiroLibreLateralEnContra)

        return changes
    }

    private fun saveEditedFinishedMatch(playerId: Int): List<String> {
        val match = editingFinishedMatch ?: return emptyList()
        val player = (match.starters + match.substitutes).firstOrNull { it.id == playerId }
            ?: return emptyList()

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

    fun savePlayerStatsDraftAsEvents(playerId: Int): List<String> {
        if (editingFinishedMatch != null) {
            return saveEditedFinishedMatch(playerId)
        }

        val match = currentMatch ?: return emptyList()
        val player = getSelectedPlayer() ?: return emptyList()

        val key = statsDraftKey(match.id, playerId)
        val draft = playerStatsDrafts[key] ?: return emptyList()
        val timestamp = getElapsedMatchTimeLabel()

        val savedLines = mutableListOf<String>()

        fun register(type: String, count: Int) {
            if (count > 0) {
                addStatEvent(
                    playerId = player.id,
                    playerName = player.name,
                    type = type,
                    count = count
                )
                savedLines.add("$type: $count $timestamp")
            }
        }

        register("Gol", draft.gol)
        register("Asistencia", draft.asistencia)
        register("Amarilla", draft.amarilla)
        register("Roja", draft.roja)
        register("Disparos al Arco", draft.disparosAlArco)
        register("Ocasiones de Gol", draft.ocasionesDeGol)
        register("Pelotas Perdidas", draft.pelotasPerdidas)
        register("Pelotas Recuperadas", draft.pelotasRecuperadas)
        register("Centros Buenos", draft.centrosBuenos)
        register("Centros Malos", draft.centrosMalos)
        register("Falta a Favor", draft.faltaAFavor)
        register("Falta en Contra", draft.faltaEnContra)
        register("Corner a Favor", draft.cornerAFavor)
        register("Corner en Contra", draft.cornerEnContra)
        register("Tiro Libre a Favor", draft.tiroLibreAFavor)
        register("Tiro Libre en Contra", draft.tiroLibreEnContra)
        register("Tiro Libre Lateral a Favor", draft.tiroLibreLateralAFavor)
        register("Tiro Libre Lateral en Contra", draft.tiroLibreLateralEnContra)

        return savedLines
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

        viewModelScope.launch {
            repository.updateMatch(updatedMatch)
        }
    }

    fun selectTeam(team: Team) {
        selectedTeamId = team.id
    }

    fun addPlayer(name: String) {
        val team = selectedTeam ?: return
        if (name.isBlank()) return
        if (team.players.size >= 30) return

        viewModelScope.launch {
            repository.addPlayer(team.id, name)
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

        viewModelScope.launch {
            repository.removePlayer(team.id, player.id)
        }
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
        return (match.starters + match.substitutes).firstOrNull { it.id == id }
    }

    private fun getElapsedMatchTimeLabel(match: MatchRecord): String {
        val elapsed = (match.totalSeconds - match.remainingSeconds).coerceAtLeast(0)
        val minutes = elapsed / 60
        val seconds = elapsed % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun stopMatch() {
        val match = currentMatch ?: return

        matchTimerJob?.cancel()

        val finishedMatch = match.copy(
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
        }
    }

    fun getElapsedMatchTimeLabel(): String {
        val match = currentMatch ?: return "00:00"
        return getElapsedMatchTimeLabel(match)
    }

    fun getTotalEventsOfCurrentMatch(): Int {
        return currentMatch?.events?.size ?: 0
    }

    fun getTotalEventsOfSelectedFinishedMatch(): Int {
        return selectedFinishedMatch?.events?.size ?: 0
    }

    fun toggleStarter(player: Player) {
        val match = currentMatch ?: return
        if (match.isStarted || match.isFinished) return

        val updatedStarters = match.starters.toMutableList()
        val updatedSubs = match.substitutes.toMutableList()

        val exists = updatedStarters.any { it.id == player.id }

        if (exists) {
            updatedStarters.removeAll { it.id == player.id }
        } else if (
            updatedStarters.size < 11 &&
            updatedSubs.none { it.id == player.id }
        ) {
            updatedStarters.add(player)
        }

        val updatedMatch = match.copy(
            starters = updatedStarters,
            substitutes = updatedSubs
        )

        currentMatch = updatedMatch

        viewModelScope.launch {
            repository.saveMatchAndLineup(updatedMatch)
        }
    }

    fun addStatEvent(
        playerId: Int? = null,
        playerName: String,
        type: String,
        count: Int
    ) {
        val match = currentMatch ?: return
        if (count <= 0) return

        val elapsed = (match.totalSeconds - match.remainingSeconds).coerceAtLeast(0)
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

        val updatedEvents = match.events.toMutableList().apply {
            add(newEvent)
        }

        val updatedMatch = match.copy(events = updatedEvents)
        currentMatch = updatedMatch

        viewModelScope.launch {
            repository.addEvent(match.id, newEvent)
        }
    }

    fun clearSelectedFinishedMatch() {
        selectedFinishedMatch = null
        clearEditingFinishedMatch()
        selectedPlayerId = null
    }

    fun toggleSubstitute(player: Player) {
        val match = currentMatch ?: return
        if (match.isStarted || match.isFinished) return

        val updatedStarters = match.starters.toMutableList()
        val updatedSubs = match.substitutes.toMutableList()

        val exists = updatedSubs.any { it.id == player.id }

        if (exists) {
            updatedSubs.removeAll { it.id == player.id }
        } else if (
            updatedSubs.size < 5 &&
            updatedStarters.none { it.id == player.id }
        ) {
            updatedSubs.add(player)
        }

        val updatedMatch = match.copy(
            starters = updatedStarters,
            substitutes = updatedSubs
        )

        currentMatch = updatedMatch

        viewModelScope.launch {
            repository.saveMatchAndLineup(updatedMatch)
        }
    }

    fun startMatch() {
        val match = currentMatch ?: return
        if (match.isStarted || match.isFinished) return

        clearEditingFinishedMatch()

        val startedMatch = match.copy(
            isStarted = true,
            isFinished = false
        )

        currentMatch = startedMatch

        viewModelScope.launch {
            repository.updateMatch(startedMatch)
        }

        matchTimerJob?.cancel()
        matchTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)

                val updated = currentMatch ?: break
                if (!updated.isStarted || updated.isFinished) break

                val newRemaining = (updated.remainingSeconds - 1).coerceAtLeast(0)

                if (newRemaining == 0) {
                    val finishedMatch = updated.copy(
                        remainingSeconds = 0,
                        isFinished = true,
                        finishedAtMillis = System.currentTimeMillis(),
                        finishedAtLabel = getElapsedMatchTimeLabel(
                            updated.copy(remainingSeconds = 0)
                        )
                    )

                    currentMatch = finishedMatch
                    putFinishedMatchLocally(finishedMatch)
                    shouldShowFinishedDialog = true
                    repository.updateMatch(finishedMatch)
                    break
                } else {
                    val tickingMatch = updated.copy(
                        remainingSeconds = newRemaining
                    )

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

        val elapsed = (match.totalSeconds - match.remainingSeconds).coerceAtLeast(0)
        val minute = elapsed / 60
        val timestamp = getElapsedMatchTimeLabel()

        val updatedStarters = match.starters.toMutableList().apply {
            removeAll { it.id == starter.id }
            add(sub)
        }

        val updatedSubstitutes = match.substitutes.toMutableList().apply {
            removeAll { it.id == sub.id }
            add(starter)
        }

        val swapEvent = MatchEvent(
            minute = minute,
            type = "Cambio",
            playerId = sub.id,
            playerName = sub.name,
            detail = "Entra ${sub.name} por ${starter.name}",
            timestampLabel = timestamp
        )

        val updatedEvents = match.events.toMutableList().apply {
            add(swapEvent)
        }

        val updatedMatch = match.copy(
            starters = updatedStarters,
            substitutes = updatedSubstitutes,
            events = updatedEvents
        )

        currentMatch = updatedMatch

        viewModelScope.launch {
            repository.saveMatchAndLineup(updatedMatch)
            repository.addEvent(updatedMatch.id, swapEvent)
        }
    }

    fun swapPlayerDuringMatch(starter: Player, substitute: Player) {
        val match = currentMatch ?: return
        if (!match.isStarted || match.isFinished) return

        val currentStarter = match.starters.firstOrNull { it.id == starter.id } ?: return
        val currentSub = match.substitutes.firstOrNull { it.id == substitute.id } ?: return

        val updatedStarters = match.starters.toMutableList().apply {
            removeAll { it.id == currentStarter.id }
            add(currentSub)
        }

        val updatedSubstitutes = match.substitutes.toMutableList().apply {
            removeAll { it.id == currentSub.id }
            add(currentStarter)
        }

        val updatedMatch = match.copy(
            starters = updatedStarters,
            substitutes = updatedSubstitutes
        )

        currentMatch = updatedMatch

        viewModelScope.launch {
            repository.saveMatchAndLineup(updatedMatch)
        }
    }

    fun getPlayerStats(playerId: Int): PlayerStats {
        val match = currentMatch ?: return PlayerStats()
        return match.statsByPlayerId.getOrPut(playerId) { PlayerStats() }
    }

    fun addCustomTeam(
        teamName: String,
        teamEmoji: String,
        playerNames: List<String>
    ) {
        if (teamName.isBlank()) return
        if (playerNames.size !in 5..30) return

        viewModelScope.launch {
            val createdTeam = repository.addCustomTeam(
                teamName = teamName,
                teamEmoji = teamEmoji,
                playerNames = playerNames.map { it.trim() }.filter { it.isNotBlank() }
            ) ?: return@launch

            selectedTeamId = createdTeam.id
        }
    }

    fun addEvent(minuteText: String, type: String, playerName: String) {
        val match = currentMatch ?: return
        val minute = minuteText.toIntOrNull() ?: 0

        val playerId = (match.starters + match.substitutes)
            .firstOrNull { it.name == playerName }
            ?.id

        val event = MatchEvent(
            minute = minute,
            type = type,
            playerId = playerId,
            playerName = playerName
        )

        val updatedEvents = match.events.toMutableList().apply {
            add(event)
        }

        currentMatch = match.copy(events = updatedEvents)

        viewModelScope.launch {
            repository.addEvent(match.id, event)
        }
    }
}