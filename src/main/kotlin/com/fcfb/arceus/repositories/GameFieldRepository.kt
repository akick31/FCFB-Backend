package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.GameField
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameFieldRepository : CrudRepository<GameField, Int>
