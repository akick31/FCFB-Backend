package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.Record
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RecordRepository : JpaRepository<Record, Long> {
    
    /**
     * Find all records
     */
    fun findAllByOrderBySeasonNumberDescWeekDesc(): List<Record>
    
    /**
     * Find record by stat name and record type
     */
    fun findByRecordNameAndRecordType(recordName: Stats, recordType: RecordType): Record?
    
    /**
     * Find all records for a specific season
     */
    fun findBySeasonNumberOrderByWeekDesc(seasonNumber: Int): List<Record>
    
    /**
     * Find all records for a specific team
     */
    fun findByRecordTeamOrderBySeasonNumberDescWeekDesc(recordTeam: String): List<Record>
    
    /**
     * Find all records for a specific stat type
     */
    fun findByRecordNameOrderByRecordValueDesc(recordName: Stats): List<Record>
    
    /**
     * Find records by game ID
     */
    fun findByGameId(gameId: Int): List<Record>
    
    /**
     * Find the current record value for a specific stat and record type
     */
    @Query("SELECT r.recordValue FROM Record r WHERE r.recordName = :recordName AND r.recordType = :recordType")
    fun findCurrentRecordValue(@Param("recordName") recordName: Stats, @Param("recordType") recordType: RecordType): Double?
    
    /**
     * Find the current record for a specific stat and record type
     */
    fun findTopByRecordNameAndRecordTypeOrderByRecordValueDesc(recordName: Stats, recordType: RecordType): Record?
    
    /**
     * Delete all records for a specific season
     */
    fun deleteBySeasonNumber(seasonNumber: Int)
    
    /**
     * Delete all records for a specific game
     */
    fun deleteByGameId(gameId: Int)
    
    /**
     * Find records that were broken in a specific game
     */
    @Query("SELECT r FROM Record r WHERE r.gameId = :gameId AND r.recordValue > COALESCE(r.previousRecordValue, 0)")
    fun findRecordsBrokenInGame(@Param("gameId") gameId: Int): List<Record>
}
