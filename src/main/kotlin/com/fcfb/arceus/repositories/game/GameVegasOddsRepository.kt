package com.fcfb.arceus.repositories.game

import com.fcfb.arceus.model.game.GameVegasOdds
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameVegasOddsRepository : CrudRepository<GameVegasOdds, Int>
