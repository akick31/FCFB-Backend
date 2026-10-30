package com.fcfb.arceus.repositories.appearance

import com.fcfb.arceus.model.appearance.GameLook
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameLookRepository : CrudRepository<GameLook, Int>
