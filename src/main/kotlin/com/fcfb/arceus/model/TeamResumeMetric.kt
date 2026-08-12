package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "team_resume_metric")
class TeamResumeMetric {
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

    @Basic
    @Column(name = "team_id")
    var teamId: Int = 0

    @Basic
    @Column(name = "overall_wins")
    var overallWins: Int = 0

    @Basic
    @Column(name = "overall_losses")
    var overallLosses: Int = 0

    @Basic
    @Column(name = "conference_wins")
    var conferenceWins: Int = 0

    @Basic
    @Column(name = "conference_losses")
    var conferenceLosses: Int = 0

    @Basic
    @Column(name = "q1_wins")
    var q1Wins: Int = 0

    @Basic
    @Column(name = "q1_losses")
    var q1Losses: Int = 0

    @Basic
    @Column(name = "th_wins")
    var thWins: Int = 0

    @Basic
    @Column(name = "th_losses")
    var thLosses: Int = 0

    @Basic
    @Column(name = "q4_wins")
    var q4Wins: Int = 0

    @Basic
    @Column(name = "q4_losses")
    var q4Losses: Int = 0

    @Basic
    @Column(name = "t25_wins")
    var t25Wins: Int = 0

    @Basic
    @Column(name = "t25_losses")
    var t25Losses: Int = 0

    @Basic
    @Column(name = "t50_wins")
    var t50Wins: Int = 0

    @Basic
    @Column(name = "t50_losses")
    var t50Losses: Int = 0

    @Basic
    @Column(name = "t100_wins")
    var t100Wins: Int = 0

    @Basic
    @Column(name = "t100_losses")
    var t100Losses: Int = 0

    @Basic
    @Column(name = "avg_opponent_composite_rank")
    var avgOpponentCompositeRank: Double? = null

    @Basic
    @Column(name = "composite_sos")
    var compositeSos: Double? = null

    constructor(
        season: Int,
        week: Int,
        teamId: Int,
        overallWins: Int,
        overallLosses: Int,
        conferenceWins: Int,
        conferenceLosses: Int,
        q1Wins: Int,
        q1Losses: Int,
        thWins: Int,
        thLosses: Int,
        q4Wins: Int,
        q4Losses: Int,
        t25Wins: Int,
        t25Losses: Int,
        t50Wins: Int,
        t50Losses: Int,
        t100Wins: Int,
        t100Losses: Int,
        avgOpponentCompositeRank: Double?,
        compositeSos: Double?,
    ) {
        this.season = season
        this.week = week
        this.teamId = teamId
        this.overallWins = overallWins
        this.overallLosses = overallLosses
        this.conferenceWins = conferenceWins
        this.conferenceLosses = conferenceLosses
        this.q1Wins = q1Wins
        this.q1Losses = q1Losses
        this.thWins = thWins
        this.thLosses = thLosses
        this.q4Wins = q4Wins
        this.q4Losses = q4Losses
        this.t25Wins = t25Wins
        this.t25Losses = t25Losses
        this.t50Wins = t50Wins
        this.t50Losses = t50Losses
        this.t100Wins = t100Wins
        this.t100Losses = t100Losses
        this.avgOpponentCompositeRank = avgOpponentCompositeRank
        this.compositeSos = compositeSos
    }

    constructor()
}
