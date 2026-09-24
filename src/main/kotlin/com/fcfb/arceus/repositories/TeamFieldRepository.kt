package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.TeamField
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamFieldRepository : CrudRepository<TeamField, String>
