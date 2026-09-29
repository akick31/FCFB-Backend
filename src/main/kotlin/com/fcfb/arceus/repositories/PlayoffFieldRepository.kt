package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.PlayoffField
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PlayoffFieldRepository : CrudRepository<PlayoffField, String>
