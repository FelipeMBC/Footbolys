package com.fmarquez.footboly.data.mapper

import com.fmarquez.footboly.data.local.entity.MatchEntity
import com.fmarquez.footboly.data.local.entity.MatchEventEntity
import com.fmarquez.footboly.data.local.relation.MatchWithDetails
import com.fmarquez.footboly.data.local.relation.TeamWithPlayers
import com.fmarquez.footboly.modelos.MatchEvent
import com.fmarquez.footboly.modelos.MatchRecord
import com.fmarquez.footboly.modelos.Player
import com.fmarquez.footboly.modelos.Team

fun TeamWithPlayers.toDomain(): Team {
    return Team(
        id = team.id,
        name = team.name,
        logoEmoji = team.logoEmoji,
        players = players
            .sortedBy { it.number }
            .map { player ->
                Player(
                    id = player.id,
                    name = player.name,
                    number = player.number
                )
            }
    )
}

fun MatchWithDetails.toDomain(): MatchRecord {
    val starters = participants
        .filter { it.role == "STARTER" }
        .sortedBy { it.playerNumberSnapshot }
        .map {
            Player(
                id = it.playerId,
                name = it.playerNameSnapshot,
                number = it.playerNumberSnapshot
            )
        }
        .toMutableList()

    val substitutes = participants
        .filter { it.role == "SUBSTITUTE" }
        .sortedBy { it.playerNumberSnapshot }
        .map {
            Player(
                id = it.playerId,
                name = it.playerNameSnapshot,
                number = it.playerNumberSnapshot
            )
        }
        .toMutableList()

    val mappedEvents = events
        .sortedBy { it.id }
        .map {
            MatchEvent(
                minute = it.minute,
                type = it.type,
                playerId = it.playerId,
                playerName = it.playerName,
                detail = it.detail,
                timestampLabel = it.timestampLabel
            )
        }
        .toMutableList()

    return MatchRecord(
        id = match.id,
        teamId = match.teamId,
        teamName = match.teamNameSnapshot,
        starters = starters,
        substitutes = substitutes,
        statsByPlayerId = mutableMapOf(),
        events = mappedEvents,
        isStarted = match.isStarted,
        isFinished = match.isFinished,
        totalSeconds = match.totalSeconds,
        remainingSeconds = match.remainingSeconds,
        createdAtMillis = match.createdAtMillis,
        finishedAtMillis = match.finishedAtMillis,
        finishedAtLabel = match.finishedAtLabel
    )
}

fun MatchRecord.toEntity(): MatchEntity {
    return MatchEntity(
        id = id,
        teamId = teamId,
        teamNameSnapshot = teamName,
        isStarted = isStarted,
        isFinished = isFinished,
        totalSeconds = totalSeconds,
        remainingSeconds = remainingSeconds,
        createdAtMillis = createdAtMillis,
        finishedAtMillis = finishedAtMillis,
        finishedAtLabel = finishedAtLabel
    )
}

fun MatchEvent.toEntity(id: Int, matchId: Int): MatchEventEntity {
    return MatchEventEntity(
        id = id,
        matchId = matchId,
        playerId = playerId,
        playerName = playerName,
        minute = minute,
        type = type,
        detail = detail,
        timestampLabel = timestampLabel
    )
}