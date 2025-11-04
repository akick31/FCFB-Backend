package com.fcfb.arceus.service.specification

import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.ConferenceStats
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

@Service
class ConferenceStatsSpecificationService {
    /**
     * Create specification for filtering conference stats
     */
    fun createSpecification(
        conference: Conference?,
        season: Int?,
        subdivision: Subdivision?,
    ): Specification<ConferenceStats> {
        return Specification { root: Root<ConferenceStats>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            conference?.let { predicates.add(cb.equal(root.get<Conference>("conference"), it)) }
            season?.let { predicates.add(cb.equal(root.get<Int>("seasonNumber"), it)) }
            subdivision?.let { predicates.add(cb.equal(root.get<Subdivision>("subdivision"), it)) }

            cb.and(*predicates.toTypedArray())
        }
    }

    /**
     * Create sort orders for conference stats
     */
    fun createSort(): List<org.springframework.data.domain.Sort.Order> {
        return listOf(
            org.springframework.data.domain.Sort.Order.desc("seasonNumber"),
            org.springframework.data.domain.Sort.Order.asc("subdivision"),
            org.springframework.data.domain.Sort.Order.asc("conference"),
        )
    }
}
