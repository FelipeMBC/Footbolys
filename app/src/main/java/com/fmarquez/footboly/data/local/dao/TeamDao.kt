package com.fmarquez.footboly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fmarquez.footboly.data.local.entity.PlayerEntity
import com.fmarquez.footboly.data.local.entity.TeamEntity
import com.fmarquez.footboly.data.local.relation.TeamWithPlayers
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {

    @Transaction
    @Query("SELECT * FROM teams ORDER BY name ASC")
    fun observeTeams(): Flow<List<TeamWithPlayers>>

    @Transaction
    @Query("SELECT * FROM teams WHERE id = :teamId LIMIT 1")
    suspend fun getTeamWithPlayers(teamId: Int): TeamWithPlayers?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity)

    @Update
    suspend fun updatePlayers(players: List<PlayerEntity>)

    @Query("DELETE FROM players WHERE id = :playerId")
    suspend fun deletePlayer(playerId: Int)

    @Query("SELECT * FROM players WHERE teamId = :teamId ORDER BY number ASC")
    suspend fun getPlayersByTeam(teamId: Int): List<PlayerEntity>

    @Query("SELECT COUNT(*) FROM teams")
    suspend fun countTeams(): Int

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM teams")
    suspend fun getNextTeamId(): Int

    @Query("SELECT COALESCE(MAX(id), 0) + 1 FROM players")
    suspend fun getNextPlayerId(): Int
}