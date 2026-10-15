package com.fcfb.arceus.repositories.game

import com.fcfb.arceus.model.game.GameRecordSnapshot
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface GameRecordSnapshotRepository : CrudRepository<GameRecordSnapshot, Int>
