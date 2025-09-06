package com.fcfb.arceus.service.specification

import com.fcfb.arceus.model.SeasonStats
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

@Service
class SeasonStatsSpecificationService {
    /**
     * Create specification for filtering season stats
     */
    fun createSpecification(
        team: String?,
        season: Int?,
    ): Specification<SeasonStats> {
        return Specification { root: Root<SeasonStats>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            team?.let { predicates.add(cb.like(cb.lower(root.get<String>("team")), "%${it.lowercase()}%")) }
            season?.let { predicates.add(cb.equal(root.get<Int>("seasonNumber"), it)) }

            cb.and(*predicates.toTypedArray())
        }
    }

    /**
     * Create sort orders for season stats
     */
    fun createSort(): List<org.springframework.data.domain.Sort.Order> {
        return listOf(
            org.springframework.data.domain.Sort.Order.desc("seasonNumber"),
            org.springframework.data.domain.Sort.Order.asc("team"),
        )
    }
}
