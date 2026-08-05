package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.records.RecordScope
import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.Record
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RecordRepository : JpaRepository<Record, Long>, JpaSpecificationExecutor<Record> {
    fun findAllByOrderBySeasonNumberDescWeekDesc(): List<Record>

    fun findByRecordNameAndRecordType(
        recordName: Stats,
        recordType: RecordType,
    ): Record?

    fun findBySeasonNumberOrderByWeekDesc(seasonNumber: Int): List<Record>

    fun findByRecordTeamOrderBySeasonNumberDescWeekDesc(recordTeam: String): List<Record>

    fun findByRecordNameOrderByRecordValueDesc(recordName: Stats): List<Record>

    fun findByGameId(gameId: Int): List<Record>

    @Query("SELECT r.recordValue FROM Record r WHERE r.recordName = :recordName AND r.recordType = :recordType")
    fun findCurrentRecordValue(
        @Param("recordName") recordName: Stats,
        @Param("recordType") recordType: RecordType,
    ): Double?

    fun findTopByRecordNameAndRecordTypeOrderByRecordValueDesc(
        recordName: Stats,
        recordType: RecordType,
    ): Record?

    @Query(
        "SELECT r FROM Record r WHERE r.recordName = :recordName AND r.recordType = :recordType " +
            "AND r.recordScope = :recordScope " +
            "AND ((:scopeValue IS NULL AND r.scopeValue IS NULL) OR r.scopeValue = :scopeValue) " +
            "ORDER BY r.recordValue DESC",
    )
    fun findScopedRecords(
        @Param("recordName") recordName: Stats,
        @Param("recordType") recordType: RecordType,
        @Param("recordScope") recordScope: RecordScope,
        @Param("scopeValue") scopeValue: String?,
    ): List<Record>

    fun deleteByRecordScopeIn(recordScopes: Collection<RecordScope>)

    fun countByRecordScope(recordScope: RecordScope): Long

    fun deleteBySeasonNumber(seasonNumber: Int)

    fun deleteByGameId(gameId: Int)

    @Query("SELECT r FROM Record r WHERE r.gameId = :gameId AND r.recordValue > COALESCE(r.previousRecordValue, 0)")
    fun findRecordsBrokenInGame(
        @Param("gameId") gameId: Int,
    ): List<Record>
}
