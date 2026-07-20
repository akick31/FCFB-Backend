package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.Offseason
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OffseasonRepository : CrudRepository<Offseason, Int> {
    @Query(value = "SELECT * FROM offseason WHERE end_date IS NULL ORDER BY id DESC LIMIT 1", nativeQuery = true)
    fun getCurrentOffseason(): Offseason?
}
