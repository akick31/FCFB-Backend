package com.fcfb.arceus.repositories.appearance

import com.fcfb.arceus.model.appearance.StadiumChangeLog
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface StadiumChangeLogRepository : CrudRepository<StadiumChangeLog, Long> {
    fun findAllByVenueOrderByChangedAtDescIdDesc(venue: String): List<StadiumChangeLog>
}
