package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.PostseasonLeagueStats
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PostseasonLeagueStatsRepository :
    CrudRepository<PostseasonLeagueStats, Int>, JpaSpecificationExecutor<PostseasonLeagueStats> {
    fun findBySubdivisionAndSeasonNumber(
        subdivision: Subdivision,
        seasonNumber: Int,
    ): PostseasonLeagueStats?
}
