package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.model.ConferenceStats
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ConferenceStatsRepository : CrudRepository<ConferenceStats, Int>, JpaSpecificationExecutor<ConferenceStats> {
    fun findAllByOrderBySeasonNumberDescSubdivisionAsc(): List<ConferenceStats>

    fun findBySubdivisionAndConferenceAndSeasonNumber(
        subdivision: com.fcfb.arceus.enums.team.Subdivision,
        conference: Conference,
        seasonNumber: Int,
    ): ConferenceStats?

    fun findBySubdivisionAndSeasonNumber(
        subdivision: com.fcfb.arceus.enums.team.Subdivision,
        seasonNumber: Int,
    ): List<ConferenceStats>

    fun findByConferenceAndSeasonNumber(
        conference: Conference,
        seasonNumber: Int,
    ): List<ConferenceStats>

    fun findBySubdivisionOrderBySeasonNumberDesc(subdivision: com.fcfb.arceus.enums.team.Subdivision): List<ConferenceStats>

    fun findByConferenceOrderBySeasonNumberDesc(conference: Conference): List<ConferenceStats>

    fun findBySeasonNumberOrderBySubdivisionAsc(seasonNumber: Int): List<ConferenceStats>

    fun existsBySubdivisionAndConferenceAndSeasonNumber(
        subdivision: com.fcfb.arceus.enums.team.Subdivision,
        conference: Conference,
        seasonNumber: Int,
    ): Boolean

    fun existsBySubdivisionAndSeasonNumber(
        subdivision: com.fcfb.arceus.enums.team.Subdivision,
        seasonNumber: Int,
    ): Boolean

    fun existsByConferenceAndSeasonNumber(
        conference: Conference,
        seasonNumber: Int,
    ): Boolean

    fun countBySubdivision(subdivision: com.fcfb.arceus.enums.team.Subdivision): Long

    fun countByConference(conference: Conference): Long

    fun countBySeasonNumber(seasonNumber: Int): Long
}
