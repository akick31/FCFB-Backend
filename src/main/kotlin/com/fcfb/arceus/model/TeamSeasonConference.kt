package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "team_season_conference")
class TeamSeasonConference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0

    @Basic
    @Column(name = "team")
    lateinit var team: String

    @Basic
    @Column(name = "season_number")
    var seasonNumber: Int = 0

    @Basic
    @Column(name = "conference")
    var conference: String? = null

    constructor(
        team: String,
        seasonNumber: Int,
        conference: String?,
    ) {
        this.team = team
        this.seasonNumber = seasonNumber
        this.conference = conference
    }

    constructor()
}
