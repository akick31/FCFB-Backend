package com.fcfb.arceus.model

import com.fcfb.arceus.enums.ranking.PollType
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "ranking")
class Ranking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int = 0

    @Basic
    @Column(name = "season")
    var season: Int = 0

    @Basic
    @Column(name = "week")
    var week: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "poll_type")
    var pollType: PollType? = null

    @Basic
    @Column(name = "poll_rank")
    var rank: Int = 0

    @Basic
    @Column(name = "team_id")
    var teamId: Int = 0

    @Basic
    @Column(name = "wins")
    var wins: Int? = null

    @Basic
    @Column(name = "losses")
    var losses: Int? = null

    constructor(
        season: Int,
        week: Int,
        pollType: PollType,
        rank: Int,
        teamId: Int,
        wins: Int?,
        losses: Int?,
    ) {
        this.season = season
        this.week = week
        this.pollType = pollType
        this.rank = rank
        this.teamId = teamId
        this.wins = wins
        this.losses = losses
    }

    constructor()
}
