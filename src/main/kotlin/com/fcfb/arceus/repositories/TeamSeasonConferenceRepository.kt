package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.TeamSeasonConference
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamSeasonConferenceRepository : CrudRepository<TeamSeasonConference, Long> {
    fun findByTeamAndSeasonNumber(
        team: String,
        seasonNumber: Int,
    ): TeamSeasonConference?

    fun findBySeasonNumber(seasonNumber: Int): List<TeamSeasonConference>
}
