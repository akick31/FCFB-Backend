package com.fcfb.arceus.repositories.appearance

import com.fcfb.arceus.model.appearance.PlayoffField
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PlayoffFieldRepository : CrudRepository<PlayoffField, String>
