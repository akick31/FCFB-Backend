package com.fcfb.arceus.service.specification

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.PlaybookStats
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

@Service
class PlaybookStatsSpecificationService {
    /**
     * Create specification for filtering playbook stats
     */
    fun createSpecification(
        offensivePlaybook: OffensivePlaybook?,
        defensivePlaybook: DefensivePlaybook?,
        season: Int?,
    ): Specification<PlaybookStats> {
        return Specification { root: Root<PlaybookStats>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            offensivePlaybook?.let { predicates.add(cb.equal(root.get<OffensivePlaybook>("offensivePlaybook"), it)) }
            defensivePlaybook?.let { predicates.add(cb.equal(root.get<DefensivePlaybook>("defensivePlaybook"), it)) }
            season?.let { predicates.add(cb.equal(root.get<Int>("seasonNumber"), it)) }

            cb.and(*predicates.toTypedArray())
        }
    }

    /**
     * Create sort orders for playbook stats
     */
    fun createSort(): List<org.springframework.data.domain.Sort.Order> {
        return listOf(
            org.springframework.data.domain.Sort.Order.desc("seasonNumber"),
            org.springframework.data.domain.Sort.Order.asc("offensivePlaybook"),
            org.springframework.data.domain.Sort.Order.asc("defensivePlaybook"),
        )
    }
}
