package com.fcfb.arceus.repositories.statistics

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.statistics.PostseasonConferenceStats
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PostseasonConferenceStatsRepository :
    CrudRepository<PostseasonConferenceStats, Int>, JpaSpecificationExecutor<PostseasonConferenceStats> {
    fun findBySubdivisionAndConferenceAndSeasonNumber(
        subdivision: Subdivision,
        conference: String,
        seasonNumber: Int,
    ): PostseasonConferenceStats?
}
