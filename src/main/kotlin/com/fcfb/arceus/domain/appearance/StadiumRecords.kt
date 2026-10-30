package com.fcfb.arceus.domain.appearance

import com.fcfb.arceus.dto.response.StadiumGameResponse
import com.fcfb.arceus.dto.response.StadiumTeamRecordResponse

object StadiumRecords {
    fun of(games: List<StadiumGameResponse>): List<StadiumTeamRecordResponse> {
        val decided = games.filter { it.final && it.homeScore != null && it.awayScore != null && it.homeScore != it.awayScore }
        val wins = decided.groupingBy { if (it.homeScore!! > it.awayScore!!) it.homeTeam else it.awayTeam }.eachCount()
        val losses = decided.groupingBy { if (it.homeScore!! > it.awayScore!!) it.awayTeam else it.homeTeam }.eachCount()
        return (wins.keys + losses.keys)
            .map { StadiumTeamRecordResponse(it, wins[it] ?: 0, losses[it] ?: 0) }
            .sortedWith(compareByDescending<StadiumTeamRecordResponse> { it.wins + it.losses }.thenBy { it.team })
    }
}
