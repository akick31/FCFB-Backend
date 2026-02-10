package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "season")
class Season {
    @Id
    @Column(name = "season_number")
    var seasonNumber: Int = 10

    @Basic
    @Column(name = "start_date")
    var startDate: String? = null

    @Basic
    @Column(name = "end_date")
    var endDate: String? = null

    @Basic
    @Column(name = "national_championship_winning_team")
    var nationalChampionshipWinningTeam: String? = null

    @Basic
    @Column(name = "national_championship_losing_team")
    var nationalChampionshipLosingTeam: String? = null

    @Basic
    @Column(name = "national_championship_winning_coach")
    var nationalChampionshipWinningCoach: String? = null

    @Basic
    @Column(name = "national_championship_losing_coach")
    var nationalChampionshipLosingCoach: String? = null

    @Basic
    @Column(name = "current_week")
    var currentWeek: Int = 1

    @Basic
    @Column(name = "current_season")
    var currentSeason: Boolean = false

    @Basic
    @Column(name = "schedule_locked")
    var scheduleLocked: Boolean = false

    constructor(
        seasonNumber: Int,
        startDate: String?,
        endDate: String?,
        nationalChampionshipWinningTeam: String?,
        nationalChampionshipLosingTeam: String?,
        nationalChampionshipWinningCoach: String?,
        nationalChampionshipLosingCoach: String?,
        currentWeek: Int,
        currentSeason: Boolean,
        scheduleLocked: Boolean = false,
    ) {
        this.seasonNumber = seasonNumber
        this.startDate = startDate
        this.endDate = endDate
        this.nationalChampionshipWinningTeam = nationalChampionshipWinningTeam
        this.nationalChampionshipLosingTeam = nationalChampionshipLosingTeam
        this.nationalChampionshipWinningCoach = nationalChampionshipWinningCoach
        this.nationalChampionshipLosingCoach = nationalChampionshipLosingCoach
        this.currentWeek = currentWeek
        this.currentSeason = currentSeason
        this.scheduleLocked = scheduleLocked
    }

    constructor()
}
