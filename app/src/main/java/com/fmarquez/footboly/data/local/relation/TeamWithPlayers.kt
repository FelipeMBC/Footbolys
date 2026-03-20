package com.fmarquez.footboly.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.fmarquez.footboly.data.local.entity.PlayerEntity
import com.fmarquez.footboly.data.local.entity.TeamEntity


data class TeamWithPlayers(
    @Embedded val team: TeamEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "teamId"
    )
    val players: List<PlayerEntity>
)