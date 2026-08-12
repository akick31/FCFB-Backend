package com.fcfb.arceus.model

import com.fcfb.arceus.enums.ranking.RankingMetricType
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
@Table(name = "ranking_metric")
class RankingMetric {
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
    @Column(name = "metric_type")
    var metricType: RankingMetricType? = null

    @Basic
    @Column(name = "team_id")
    var teamId: Int = 0

    @Basic
    @Column(name = "value")
    var value: Double = 0.0

    @Basic
    @Column(name = "wins")
    var wins: Int? = null

    @Basic
    @Column(name = "losses")
    var losses: Int? = null

    constructor(
        season: Int,
        week: Int,
        metricType: RankingMetricType,
        teamId: Int,
        value: Double,
        wins: Int?,
        losses: Int?,
    ) {
        this.season = season
        this.week = week
        this.metricType = metricType
        this.teamId = teamId
        this.value = value
        this.wins = wins
        this.losses = losses
    }

    constructor()
}
