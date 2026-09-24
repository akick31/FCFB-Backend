package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.TeamUniformCurrent
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamUniformCurrentRepository : CrudRepository<TeamUniformCurrent, String>
