package com.fcfb.arceus.repositories.game

import com.fcfb.arceus.model.game.GameCoinToss
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameCoinTossRepository : CrudRepository<GameCoinToss, Int>
