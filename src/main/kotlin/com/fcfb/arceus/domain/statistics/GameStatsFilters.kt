package com.fcfb.arceus.domain.statistics

import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.model.statistics.GameStats

class GameStatsFilters {
    fun excludingScrimmages(gameStatsList: List<GameStats>): List<GameStats> = gameStatsList.filter { it.gameType != GameType.SCRIMMAGE }
}
