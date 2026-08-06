package com.fcfb.arceus.service.specification

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.PostseasonLeagueStats
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

@Service
class PostseasonLeagueStatsSpecificationService {
    fun createSpecification(
        subdivision: Subdivision?,
        season: Int?,
    ): Specification<PostseasonLeagueStats> {
        return Specification { root: Root<PostseasonLeagueStats>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            subdivision?.let { predicates.add(cb.equal(root.get<Subdivision>("subdivision"), it)) }
            season?.let { predicates.add(cb.equal(root.get<Int>("seasonNumber"), it)) }

            cb.and(*predicates.toTypedArray())
        }
    }

    fun createSort(): List<org.springframework.data.domain.Sort.Order> {
        return listOf(
            org.springframework.data.domain.Sort.Order.desc("seasonNumber"),
            org.springframework.data.domain.Sort.Order.asc("subdivision"),
        )
    }
}
