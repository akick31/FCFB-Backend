package com.fcfb.arceus.repositories.conference

import com.fcfb.arceus.model.conference.TeamSeasonConference
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
