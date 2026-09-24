package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.TeamUniformHistory
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamUniformHistoryRepository : CrudRepository<TeamUniformHistory, Long> {
    fun findByTeamAndSeasonNumberAndWeek(
        team: String,
        seasonNumber: Int,
        week: Int,
    ): TeamUniformHistory?
}
