package com.fcfb.arceus.repositories.appearance

import com.fcfb.arceus.model.appearance.TeamField
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamFieldRepository : CrudRepository<TeamField, String>
