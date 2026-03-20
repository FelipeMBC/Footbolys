package com.fmarquez.footboly.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fmarquez.footboly.datos.mockTeams
import com.fmarquez.footboly.modelos.MatchEvent
import com.fmarquez.footboly.modelos.MatchRecord
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.modelos.PlayerStats
import com.fmarquez.footboly.modelos.PlayerStatsDraft
import com.fmarquez.footboly.modelos.Team
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FutbolViewModel : ViewModel() {

    val teams = mutableStateListOf<Team>().apply {
        addAll(mockTeams())
    }

    var editingFinishedMatch by mutableStateOf<MatchRecord?>(null)
        private set

    var selectedTeam by mutableStateOf<Team?>(null)
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

    private var matchIdCounter = 1
    private var nextPlayerId = 1000

    val finishedMatches = mutableStateListOf<MatchRecord>()

    private var matchTimerJob: Job? = null

    private val playerStatsDrafts = mutableStateMapOf<String, PlayerStatsDraft>()
    private val originalPlayerStatsDrafts = mutableStateMapOf<String, PlayerStatsDraft>()

    private fun statsDraftKey(matchId: Int, playerId: Int): String {
        return "${matchId}_$playerId"
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
        val index = finishedMatches.indexOfFirst { it.id == updatedMatch.id }
        if (index != -1) {
            finishedMatches[index] = updatedMatch
        }

        if (selectedFinishedMatch?.id == updatedMatch.id) {
            selectedFinishedMatch = updatedMatch
        }

        if (editingFinishedMatch?.id == updatedMatch.id) {
            editingFinishedMatch = updatedMatch
        }

        if (currentMatch?.id == updatedMatch.id) {
            currentMatch = updatedMatch
        }
    }

    fun deleteFinishedMatch(matchId: Int) {
        val index = finishedMatches.indexOfFirst { it.id == matchId }
        if (index != -1) {
            finishedMatches.removeAt(index)
        }

        if (selectedFinishedMatch?.id == matchId) {
            selectedFinishedMatch = null
        }

        if (editingFinishedMatch?.id == matchId) {
            editingFinishedMatch = null
        }

        if (currentMatch?.id == matchId) {
            currentMatch = null
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

        val playerEvents = match.events.filter { it.playerName == player.name }

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
        playerName: String,
        type: String,
        count: Int,
        timestampLabel: String,
        minute: Int
    ): MatchEvent {
        return MatchEvent(
            minute = minute,
            type = type,
            playerName = playerName,
            detail = "$type: $count",
            timestampLabel = timestampLabel
        )
    }

    private fun buildEventsFromDraftForPlayer(
        playerName: String,
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
                    playerName = playerName,
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
        val player = getSelectedPlayer() ?: return emptyList()

        val key = statsDraftKey(match.id, playerId)
        val updatedDraft = playerStatsDrafts[key] ?: return emptyList()
        val originalDraft = originalPlayerStatsDrafts[key] ?: buildDraftFromMatch(match, playerId)

        val changes = buildEditChanges(player.name, originalDraft, updatedDraft)

        val oldEventsOfPlayer = match.events.filter { it.playerName == player.name }
        val eventsWithoutPlayer = match.events.filterNot { it.playerName == player.name }.toMutableList()

        val rebuiltEventsForPlayer = buildEventsFromDraftForPlayer(
            playerName = player.name,
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

        currentMatch = match.copy(
            totalSeconds = safeMinutes * 60,
            remainingSeconds = safeMinutes * 60
        )
    }

    fun selectTeam(team: Team) {
        selectedTeam = team
    }

    fun addPlayer(name: String) {
        val team = selectedTeam ?: return
        if (name.isBlank()) return
        if (team.players.size >= 30) return

        val updatedPlayers = team.players.toMutableList().apply {
            add(
                Player(
                    id = nextPlayerId++,
                    name = name.trim(),
                    number = 0
                )
            )
        }.mapIndexed { index, player ->
            player.copy(number = index + 1)
        }.toMutableList()

        val updatedTeam = team.copy(players = updatedPlayers)

        val index = teams.indexOfFirst { it.id == team.id }
        if (index != -1) {
            teams[index] = updatedTeam
        }

        selectedTeam = updatedTeam
    }

    fun selectFinishedMatch(match: MatchRecord) {
        selectedFinishedMatch = match
    }

    fun dismissFinishedDialog() {
        shouldShowFinishedDialog = false
    }

    fun removePlayer(player: Player) {
        val team = selectedTeam ?: return

        val updatedPlayers = team.players
            .filter { it.id != player.id }
            .mapIndexed { index, currentPlayer ->
                currentPlayer.copy(number = index + 1)
            }
            .toMutableList()

        val updatedTeam = team.copy(players = updatedPlayers)

        val index = teams.indexOfFirst { it.id == team.id }
        if (index != -1) {
            teams[index] = updatedTeam
        }

        selectedTeam = updatedTeam
    }

    fun createNewMatch() {
        val team = selectedTeam ?: return

        clearEditingFinishedMatch()

        currentMatch = MatchRecord(
            id = matchIdCounter++,
            teamName = team.name,
            starters = mutableListOf(),
            substitutes = mutableListOf(),
            statsByPlayerId = mutableMapOf(),
            events = mutableListOf(),
            isStarted = false,
            isFinished = false,
            totalSeconds = 60,
            remainingSeconds = 60
        )

        selectedPlayerId = null
        matchTimerJob?.cancel()
    }

    fun selectPlayerForStats(playerId: Int) {
        selectedPlayerId = playerId
    }

    fun getSelectedPlayer(): Player? {
        val match = getActiveMatchForStatsInternal() ?: return null
        val id = selectedPlayerId ?: return null
        return (match.starters + match.substitutes).firstOrNull { it.id == id }
    }

    fun stopMatch() {
        val match = currentMatch ?: return

        matchTimerJob?.cancel()

        val finished = match.copy(
            isFinished = true,
            remainingSeconds = 0,
            finishedAtLabel = getElapsedMatchTimeLabel()
        )

        currentMatch = finished
        finishedMatches.add(0, finished)
        shouldShowFinishedDialog = true
    }

    fun getElapsedMatchTimeLabel(): String {
        val match = currentMatch ?: return "00:00"
        val elapsed = (match.totalSeconds - match.remainingSeconds).coerceAtLeast(0)
        val minutes = elapsed / 60
        val seconds = elapsed % 60
        return String.format("%02d:%02d", minutes, seconds)
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

        currentMatch = match.copy(
            starters = updatedStarters,
            substitutes = updatedSubs
        )
    }

    fun addStatEvent(
        playerName: String,
        type: String,
        count: Int
    ) {
        val match = currentMatch ?: return
        if (count <= 0) return

        val elapsed = (match.totalSeconds - match.remainingSeconds).coerceAtLeast(0)
        val minute = elapsed / 60
        val timestamp = getElapsedMatchTimeLabel()

        val updatedEvents = match.events.toMutableList().apply {
            add(
                MatchEvent(
                    minute = minute,
                    type = type,
                    playerName = playerName,
                    detail = "$type: $count",
                    timestampLabel = timestamp
                )
            )
        }

        currentMatch = match.copy(events = updatedEvents)
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

        currentMatch = match.copy(
            starters = updatedStarters,
            substitutes = updatedSubs
        )
    }

    fun startMatch() {
        val match = currentMatch ?: return
        if (match.isStarted || match.isFinished) return

        clearEditingFinishedMatch()

        currentMatch = match.copy(
            isStarted = true,
            isFinished = false
        )

        matchTimerJob?.cancel()
        matchTimerJob = viewModelScope.launch {
            while (true) {
                val current = currentMatch ?: break

                if (current.remainingSeconds <= 0) {
                    val finishedMatch = current.copy(
                        remainingSeconds = 0,
                        isFinished = true,
                        finishedAtLabel = "00:00"
                    )

                    currentMatch = finishedMatch
                    finishedMatches.add(0, finishedMatch)
                    shouldShowFinishedDialog = true
                    break
                }

                delay(1000)

                val updated = currentMatch ?: break
                if (updated.isStarted && !updated.isFinished) {
                    val newRemaining = (updated.remainingSeconds - 1).coerceAtLeast(0)

                    if (newRemaining == 0) {
                        val finishedMatch = updated.copy(
                            remainingSeconds = 0,
                            isFinished = true,
                            finishedAtLabel = "00:00"
                        )
                        currentMatch = finishedMatch
                        finishedMatches.add(0, finishedMatch)
                        shouldShowFinishedDialog = true
                        break
                    } else {
                        currentMatch = updated.copy(
                            remainingSeconds = newRemaining
                        )
                    }
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

    fun registerSwap(starter: Player, sub: Player, minute: Int) {
        val match = currentMatch ?: return

        match.starters.removeAll { it.id == starter.id }
        match.substitutes.removeAll { it.id == sub.id }

        match.starters.add(sub)
        match.substitutes.add(starter)

        match.events.add(
            MatchEvent(
                minute = minute,
                type = "Cambio",
                playerName = sub.name,
                detail = "Entra ${sub.name} por ${starter.name}"
            )
        )

        currentMatch = match.copy(
            starters = match.starters,
            substitutes = match.substitutes,
            events = match.events
        )
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

        currentMatch = match.copy(
            starters = updatedStarters,
            substitutes = updatedSubstitutes
        )
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
        if (playerNames.size !in 11..30) return

        val newTeamId = (teams.maxOfOrNull { it.id } ?: 0) + 1
        var nextGeneratedPlayerId = (teams.flatMap { it.players }.maxOfOrNull { it.id } ?: 0) + 1

        val generatedPlayers = playerNames.map { playerName ->
            Player(
                id = nextGeneratedPlayerId++,
                name = playerName.trim(),
                number = 0
            )
        }.mapIndexed { index, player ->
            player.copy(number = index + 1)
        }.toMutableList()

        val finalEmoji = if (teamEmoji.isBlank()) "⚽" else teamEmoji

        val newTeam = Team(
            id = newTeamId,
            name = teamName.trim(),
            logoEmoji = finalEmoji,
            players = generatedPlayers
        )

        teams.add(newTeam)
        selectedTeam = newTeam
    }

    fun addEvent(minuteText: String, type: String, playerName: String) {
        val match = currentMatch ?: return
        val minute = minuteText.toIntOrNull() ?: 0
        match.events.add(
            MatchEvent(
                minute = minute,
                type = type,
                playerName = playerName
            )
        )
        currentMatch = match.copy(events = match.events)
    }
}