package com.fmarquez.footboly.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fmarquez.footboly.datos.mockTeams
import com.fmarquez.footboly.modelos.MatchEvent
import com.fmarquez.footboly.modelos.MatchRecord
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.modelos.PlayerStats
import androidx.compose.runtime.mutableStateListOf
import com.fmarquez.footboly.modelos.Team
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch



class FutbolViewModel : ViewModel() {

    val teams = mutableStateListOf<Team>().apply {
        addAll(mockTeams())
    }

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

    private var matchIdCounter = 1
    private var nextPlayerId = 1000

    val finishedMatches = mutableStateListOf<MatchRecord>()

    private var matchTimerJob: Job? = null



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

    fun selectFinishedMatch(match: MatchRecord){
        selectedFinishedMatch = match
    }

    fun dismissFinishedDialog(){
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
        val match = currentMatch ?: return null
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