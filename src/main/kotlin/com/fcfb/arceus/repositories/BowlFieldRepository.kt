package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.BowlField
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface BowlFieldRepository : CrudRepository<BowlField, String>
