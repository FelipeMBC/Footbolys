package com.fmarquez.footboly.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.fmarquez.footboly.data.local.entity.MatchEntity
import com.fmarquez.footboly.data.local.entity.MatchEventEntity
import com.fmarquez.footboly.data.local.entity.MatchPlayerEntity

data class MatchWithDetails(
    @Embedded val match: MatchEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "matchId"
    )
    val participants: List<MatchPlayerEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "matchId"
    )
    val events: List<MatchEventEntity>
)