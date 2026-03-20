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
                    logoEmoji = team.logoEmoji
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
        playerNames: List<String>
    ): Team? {
        val cleanedTeamName = teamName.trim()
        val cleanedPlayers = playerNames
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (cleanedTeamName.isBlank()) return null
        if (cleanedPlayers.size !in 5..30) return null

        val teamId = teamDao.getNextTeamId()
        var nextPlayerId = teamDao.getNextPlayerId()

        val finalEmoji = if (teamEmoji.isBlank()) "⚽" else teamEmoji.trim()

        teamDao.insertTeam(
            TeamEntity(
                id = teamId,
                name = cleanedTeamName,
                logoEmoji = finalEmoji
            )
        )

        teamDao.insertPlayers(
            cleanedPlayers.mapIndexed { index, playerName ->
                PlayerEntity(
                    id = nextPlayerId++,
                    teamId = teamId,
                    name = playerName,
                    number = index + 1
                )
            }
        )

        return teamDao.getTeamWithPlayers(teamId)?.toDomain()
    }

    suspend fun addPlayer(teamId: Int, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        val currentPlayers = teamDao.getPlayersByTeam(teamId)
        val nextPlayerId = teamDao.getNextPlayerId()

        teamDao.insertPlayer(
            PlayerEntity(
                id = nextPlayerId,
                teamId = teamId,
                name = trimmed,
                number = currentPlayers.size + 1
            )
        )
    }

    suspend fun removePlayer(teamId: Int, playerId: Int) {
        teamDao.deletePlayer(playerId)

        val reordered = teamDao.getPlayersByTeam(teamId)
            .sortedBy { it.number }
            .mapIndexed { index, player ->
                player.copy(number = index + 1)
            }

        if (reordered.isNotEmpty()) {
            teamDao.updatePlayers(reordered)
        }
    }

    suspend fun createNewMatch(team: Team): MatchRecord {
        val matchId = matchDao.getNextMatchId()

        val matchEntity = MatchEntity(
            id = matchId,
            teamId = team.id,
            teamNameSnapshot = team.name,
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