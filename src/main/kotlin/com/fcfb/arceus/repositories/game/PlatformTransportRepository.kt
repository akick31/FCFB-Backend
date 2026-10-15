package com.fcfb.arceus.repositories.game

import com.fcfb.arceus.model.game.PlatformTransport
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PlatformTransportRepository : CrudRepository<PlatformTransport, Int>
