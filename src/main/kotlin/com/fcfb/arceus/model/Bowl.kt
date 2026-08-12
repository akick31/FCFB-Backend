package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "bowl")
class Bowl {
    @Id
    @Column(name = "name")
    lateinit var name: String

    @Basic
    @Column(name = "logo")
    var logo: String? = null

    @Basic
    @Column(name = "last_season")
    var lastSeason: Int? = null

    @Basic
    @Column(name = "last_home_team")
    var lastHomeTeam: String? = null

    @Basic
    @Column(name = "last_away_team")
    var lastAwayTeam: String? = null

    @Basic
    @Column(name = "last_home_score")
    var lastHomeScore: Int? = null

    @Basic
    @Column(name = "last_away_score")
    var lastAwayScore: Int? = null

    @Basic
    @Column(name = "last_game_id")
    var lastGameId: Int? = null

    @Basic
    @Column(name = "last_venue")
    var lastVenue: String? = null

    constructor(name: String) {
        this.name = name
    }

    constructor()
}
