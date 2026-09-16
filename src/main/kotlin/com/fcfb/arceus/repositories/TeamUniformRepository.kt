package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.TeamUniform
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamUniformRepository : CrudRepository<TeamUniform, Long> {
    fun findByTeamAndSeasonNumberAndWeek(
        team: String,
        seasonNumber: Int,
        week: Int,
    ): TeamUniform?
}
