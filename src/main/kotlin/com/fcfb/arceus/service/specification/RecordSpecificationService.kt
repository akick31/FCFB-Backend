package com.fcfb.arceus.service.specification

import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.Record
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

@Service
class RecordSpecificationService {
    /**
     * Create specification for filtering records
     */
    fun createSpecification(
        season: Int?,
        conference: String?,
        recordType: RecordType?,
        recordName: Stats?,
    ): Specification<Record> {
        return Specification { root: Root<Record>, _: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            season?.let { predicates.add(cb.equal(root.get<Int>("seasonNumber"), it)) }
            conference?.let { predicates.add(cb.like(cb.lower(root.get<String>("conference")), "%${it.lowercase()}%")) }
            recordType?.let { predicates.add(cb.equal(root.get<RecordType>("recordType"), it)) }
            recordName?.let { predicates.add(cb.equal(root.get<Stats>("recordName"), it)) }

            cb.and(*predicates.toTypedArray())
        }
    }

    /**
     * Create sort orders for records
     */
    fun createSort(): List<org.springframework.data.domain.Sort.Order> {
        return listOf(
            org.springframework.data.domain.Sort.Order.desc("seasonNumber"),
            org.springframework.data.domain.Sort.Order.asc("conference"),
            org.springframework.data.domain.Sort.Order.asc("recordType"),
            org.springframework.data.domain.Sort.Order.asc("recordName"),
        )
    }
}
