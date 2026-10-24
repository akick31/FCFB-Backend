package com.fcfb.arceus.domain.appearance

import com.fcfb.arceus.dto.response.StadiumResponse
import com.fcfb.arceus.model.appearance.BowlField
import com.fcfb.arceus.model.appearance.PlayoffField
import com.fcfb.arceus.model.appearance.Venue
import com.fcfb.arceus.model.conference.Conference
import com.fcfb.arceus.model.team.Team

class StadiumUsage(
    teams: List<Team>,
    bowls: List<BowlField>,
    conferences: List<Conference>,
    playoffRounds: List<PlayoffField>,
) {
    private val teamsByVenue = teams.filter { it.stadium != null }.groupBy({ it.stadium!! }, { it.name.orEmpty() })
    private val bowlsByVenue = bowls.filter { it.venue != null }.groupBy({ it.venue!! }, { it.bowl })
    private val conferencesByVenue = conferences.filter { it.championshipVenue != null }.groupBy({ it.championshipVenue!! }, { it.label })
    private val roundsByVenue = playoffRounds.filter { it.venue != null }.groupBy({ it.venue!! }, { it.round })

    fun describe(venue: Venue): StadiumResponse =
        StadiumResponse(
            name = venue.name,
            city = venue.city,
            state = venue.state,
            capacity = venue.capacity,
            active = venue.active,
            design = venue.design,
            teams = teamsByVenue[venue.name].orEmpty(),
            bowls = bowlsByVenue[venue.name].orEmpty(),
            conferences = conferencesByVenue[venue.name].orEmpty(),
            playoffRounds = roundsByVenue[venue.name].orEmpty(),
        )
}
