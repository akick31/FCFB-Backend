package com.fcfb.arceus.repositories.appearance

import com.fcfb.arceus.model.appearance.GameField
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameFieldRepository : CrudRepository<GameField, Int>
