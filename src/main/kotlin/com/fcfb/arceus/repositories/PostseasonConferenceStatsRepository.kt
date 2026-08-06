package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.PostseasonConferenceStats
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
