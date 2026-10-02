package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.BowlField
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
interface BowlFieldRepository : CrudRepository<BowlField, String> {
    @Transactional
    @Modifying
    @Query(value = "UPDATE bowl_field SET bowl = ?2 WHERE bowl = ?1", nativeQuery = true)
    fun renameBowl(
        oldName: String,
        newName: String,
    )
}
