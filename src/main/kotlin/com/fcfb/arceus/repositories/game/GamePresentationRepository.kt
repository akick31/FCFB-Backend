package com.fcfb.arceus.repositories.game

import com.fcfb.arceus.model.game.GamePresentation
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GamePresentationRepository : CrudRepository<GamePresentation, Int>
