package com.fcfb.arceus.service.specification

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.PostseasonPlaybookStats
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

@Service
class PostseasonPlaybookStatsSpecificationService {
    fun createSpecification(
        offensivePlaybook: OffensivePlaybook?,
        defensivePlaybook: DefensivePlaybook?,
        season: Int?,
    ): Specification<PostseasonPlaybookStats> {
        return Specification { root: Root<PostseasonPlaybookStats>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            offensivePlaybook?.let { predicates.add(cb.equal(root.get<OffensivePlaybook>("offensivePlaybook"), it)) }
            defensivePlaybook?.let { predicates.add(cb.equal(root.get<DefensivePlaybook>("defensivePlaybook"), it)) }
            season?.let { predicates.add(cb.equal(root.get<Int>("seasonNumber"), it)) }

            cb.and(*predicates.toTypedArray())
        }
    }

    fun createSort(): List<org.springframework.data.domain.Sort.Order> {
        return listOf(
            org.springframework.data.domain.Sort.Order.desc("seasonNumber"),
            org.springframework.data.domain.Sort.Order.asc("offensivePlaybook"),
            org.springframework.data.domain.Sort.Order.asc("defensivePlaybook"),
        )
    }
}
