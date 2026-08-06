package com.fcfb.arceus.service.specification

import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.model.PostseasonConferenceStats
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

@Service
class PostseasonConferenceStatsSpecificationService {
    fun createSpecification(
        conference: String?,
        season: Int?,
        subdivision: Subdivision?,
    ): Specification<PostseasonConferenceStats> {
        return Specification { root: Root<PostseasonConferenceStats>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            conference?.let { predicates.add(cb.equal(root.get<String>("conference"), it)) }
            season?.let { predicates.add(cb.equal(root.get<Int>("seasonNumber"), it)) }
            subdivision?.let { predicates.add(cb.equal(root.get<Subdivision>("subdivision"), it)) }

            cb.and(*predicates.toTypedArray())
        }
    }

    fun createSort(): List<org.springframework.data.domain.Sort.Order> {
        return listOf(
            org.springframework.data.domain.Sort.Order.desc("seasonNumber"),
            org.springframework.data.domain.Sort.Order.asc("subdivision"),
            org.springframework.data.domain.Sort.Order.asc("conference"),
        )
    }
}
