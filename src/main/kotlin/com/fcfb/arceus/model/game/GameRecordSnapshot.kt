package com.fcfb.arceus.model.game

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "game_record_snapshot")
class GameRecordSnapshot(
    @Id
    @Column(name = "game_id")
    var gameId: Int = 0,
    @Basic
    @Column(name = "home_team_rank")
    var homeTeamRank: Int? = null,
    @Basic
    @Column(name = "away_team_rank")
    var awayTeamRank: Int? = null,
    @Basic
    @Column(name = "home_wins")
    var homeWins: Int? = null,
    @Basic
    @Column(name = "home_losses")
    var homeLosses: Int? = null,
    @Basic
    @Column(name = "away_wins")
    var awayWins: Int? = null,
    @Basic
    @Column(name = "away_losses")
    var awayLosses: Int? = null,
)
