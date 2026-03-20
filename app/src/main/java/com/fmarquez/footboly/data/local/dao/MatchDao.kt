package com.fmarquez.footboly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fmarquez.footboly.data.local.entity.MatchEntity
import com.fmarquez.footboly.data.local.entity.MatchEventEntity
import com.fmarquez.footboly.data.local.entity.MatchPlayerEntity
import com.fmarquez.footboly.data.local.relation.MatchWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {

    @Transaction
    @Query("SELECT * FROM matches ORDER BY id DESC LIMIT 1")
    fun observeLatestMatch(): Flow<MatchWithDetails?>

    @Transaction
    @Query("SELECT * FROM matches WHERE isFinished = 1 ORDER BY id DESC")
    fun observeFinishedMatches(): Flow<List<MatchWithDetails>>

    @Transaction
    @Query("SELECT * FROM matches WHERE id = :matchId LIMIT 1")
    suspend fun getMatchWithDetails(matchId: Int): MatchWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchPlayers(players: List<MatchPlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: MatchEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<MatchEventEntity>)

    @Query("DELETE FROM match_players WHERE matchId = :matchId")
    suspend fun deleteMatchPlayersByMatchId(matchId: Int)

    @Query("DELETE FROM match_events WHERE matchId = :matchId AND playerId = :playerId")
    suspend fun deleteEventsForPlayer(matchId: Int, playerId: Int)

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatch(matchId: Int)

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM matches")
    suspend fun getNextMatchId(): Int

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM match_events")
    suspend fun getNextEventId(): Int
}