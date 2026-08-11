package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.Bowl
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BowlRepository : CrudRepository<Bowl, String>
