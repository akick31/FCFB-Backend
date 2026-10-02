package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.Bowl
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.transaction.Transactional

@Repository
interface BowlRepository : CrudRepository<Bowl, String> {
    @Transactional
    @Modifying
    @Query(value = "UPDATE bowl SET name = ?2 WHERE name = ?1", nativeQuery = true)
    fun renameBowl(
        oldName: String,
        newName: String,
    )

    @Transactional
    @Modifying
    @Query(value = "UPDATE bowl SET logo = ?2 WHERE name = ?1", nativeQuery = true)
    fun updateLogo(
        name: String,
        logo: String?,
    )
}
