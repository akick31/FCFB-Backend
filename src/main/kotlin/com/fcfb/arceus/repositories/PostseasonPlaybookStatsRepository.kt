package com.fcfb.arceus.repositories

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.PostseasonPlaybookStats
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PostseasonPlaybookStatsRepository :
    CrudRepository<PostseasonPlaybookStats, Int>, JpaSpecificationExecutor<PostseasonPlaybookStats> {
    fun findByOffensivePlaybookAndDefensivePlaybookAndSeasonNumber(
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        seasonNumber: Int,
    ): PostseasonPlaybookStats?
}
