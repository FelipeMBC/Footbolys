package com.fmarquez.footboly.data.repository

import com.fmarquez.footboly.data.local.dao.MatchDao
import com.fmarquez.footboly.data.local.dao.TeamDao
import com.fmarquez.footboly.data.local.entity.MatchEntity
import com.fmarquez.footboly.data.local.entity.MatchPlayerEntity
import com.fmarquez.footboly.data.local.entity.PlayerEntity
import com.fmarquez.footboly.data.local.entity.TeamEntity
import com.fmarquez.footboly.data.mapper.toDomain
import com.fmarquez.footboly.data.mapper.toEntity
import com.fmarquez.footboly.datos.mockTeams
import com.fmarquez.footboly.dialog.TempPlayerInput
import com.fmarquez.footboly.modelos.MatchEvent
import com.fmarquez.footboly.modelos.MatchRecord
import com.fmarquez.footboly.modelos.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FootballRepository(
    private val teamDao: TeamDao,
    private val matchDao: MatchDao
) {

    fun observeTeams(): Flow<List<Team>> {
        return teamDao.observeTeams().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeLatestMatch(): Flow<MatchRecord?> {
        return matchDao.observeLatestMatch().map { matchWithDetails ->
            matchWithDetails?.toDomain()
        }
    }

    fun observeFinishedMatches(): Flow<List<MatchRecord>> {
        return matchDao.observeFinishedMatches().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun seedIfNeeded() {
        if (teamDao.countTeams() > 0) return

        mockTeams().forEach { team ->
            teamDao.insertTeam(
                TeamEntity(
                    id = team.id,
                    name = team.name,
                    logoEmoji = team.logoEmoji,
                    logoUri = team.logoUri,
                    shirtColorHex = team.shirtColorHex
                )
            )

            teamDao.insertPlayers(
                team.players.map { player ->
                    PlayerEntity(
                        id = player.id,
                        teamId = team.id,
                        name = player.name,
                        number = player.number
                    )
                }
            )
        }
    }

    suspend fun addCustomTeam(
        teamName: String,
        teamEmoji: String,
        players: List<TempPlayerInput>,
        logoUri: String? = null,
        shirtColorHex: String = "#1E6B45"
    ): Team? {
        val cleanedTeamName = teamName.trim()

        val cleanedPlayers = players
            .mapNotNull { player ->
                val cleanedName = player.name.trim()
                val cleanedNumber = player.number
                if (cleanedName.isBlank() || cleanedNumber <= 0) null
                else TempPlayerInput(cleanedName, cleanedNumber)
            }
            .distinctBy { it.number }

        if (cleanedTeamName.isBlank()) return null
        if (cleanedPlayers.size !in 5..30) return null

        val teamId = teamDao.getNextTeamId()
        var nextPlayerId = teamDao.getNextPlayerId()

        val finalEmoji = if (teamEmoji.isBlank()) "⚽" else teamEmoji.trim()
        val finalShirtColor = shirtColorHex.ifBlank { "#1E6B45" }

        teamDao.insertTeam(
            TeamEntity(
                id = teamId,
                name = cleanedTeamName,
                logoEmoji = finalEmoji,
                logoUri = logoUri,
                shirtColorHex = finalShirtColor
            )
        )

        teamDao.insertPlayers(
            cleanedPlayers.map { player ->
                PlayerEntity(
                    id = nextPlayerId++,
                    teamId = teamId,
                    name = player.name,
                    number = player.number
                )
            }
        )

        return teamDao.getTeamWithPlayers(teamId)?.toDomain()
    }

    suspend fun addPlayer(teamId: Int, name: String, number: Int) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        if (number <= 0) return

        val currentPlayers = teamDao.getPlayersByTeam(teamId)
        if (currentPlayers.any { it.number == number }) return

        val nextPlayerId = teamDao.getNextPlayerId()

        teamDao.insertPlayer(
            PlayerEntity(
                id = nextPlayerId,
                teamId = teamId,
                name = trimmed,
                number = number
            )
        )
    }

    suspend fun removePlayer(teamId: Int, playerId: Int) {
        teamDao.deletePlayer(playerId)
    }

    suspend fun createNewMatch(team: Team): MatchRecord {
        val matchId = matchDao.getNextMatchId()

        val matchEntity = MatchEntity(
            id = matchId,
            teamId = team.id,
            teamNameSnapshot = team.name,
            shirtColorHex = team.shirtColorHex,
            isStarted = false,
            isFinished = false,
            totalSeconds = 60,
            remainingSeconds = 60,
            createdAtMillis = System.currentTimeMillis(),
            finishedAtMillis = null,
            finishedAtLabel = ""
        )

        matchDao.insertMatch(matchEntity)

        return requireNotNull(matchDao.getMatchWithDetails(matchId)).toDomain()
    }

    suspend fun updateMatch(match: MatchRecord) {
        matchDao.updateMatch(match.toEntity())
    }

    suspend fun saveLineup(match: MatchRecord) {
        matchDao.deleteMatchPlayersByMatchId(match.id)

        val players = buildList {
            addAll(
                match.starters.sortedBy { it.number }.map { player ->
                    MatchPlayerEntity(
                        matchId = match.id,
                        playerId = player.id,
                        role = "STARTER",
                        playerNameSnapshot = player.name,
                        playerNumberSnapshot = player.number
                    )
                }
            )
            addAll(
                match.substitutes.sortedBy { it.number }.map { player ->
                    MatchPlayerEntity(
                        matchId = match.id,
                        playerId = player.id,
                        role = "SUBSTITUTE",
                        playerNameSnapshot = player.name,
                        playerNumberSnapshot = player.number
                    )
                }
            )
        }

        if (players.isNotEmpty()) {
            matchDao.insertMatchPlayers(players)
        }
    }

    suspend fun saveMatchAndLineup(match: MatchRecord) {
        updateMatch(match)
        saveLineup(match)
    }

    suspend fun addEvent(matchId: Int, event: MatchEvent) {
        val nextEventId = matchDao.getNextEventId()
        matchDao.insertEvent(event.toEntity(nextEventId, matchId))
    }

    suspend fun addEvents(matchId: Int, events: List<MatchEvent>) {
        if (events.isEmpty()) return
        var nextEventId = matchDao.getNextEventId()
        matchDao.insertEvents(
            events.map { event ->
                event.toEntity(nextEventId++, matchId)
            }
        )
    }

    suspend fun replacePlayerEvents(
        matchId: Int,
        playerId: Int,
        events: List<MatchEvent>
    ) {
        matchDao.deleteEventsForPlayer(matchId, playerId)

        if (events.isEmpty()) return

        var nextEventId = matchDao.getNextEventId()

        matchDao.insertEvents(
            events.map { event ->
                event.toEntity(nextEventId++, matchId)
            }
        )
    }

    suspend fun deleteMatch(matchId: Int) {
        matchDao.deleteMatch(matchId)
    }
}