package com.fcfb.arceus.repositories.appearance

import com.fcfb.arceus.model.appearance.TeamUniformCurrent
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamUniformCurrentRepository : CrudRepository<TeamUniformCurrent, String>
