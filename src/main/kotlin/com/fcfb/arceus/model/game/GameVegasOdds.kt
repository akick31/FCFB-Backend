package com.fcfb.arceus.model.game

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "game_vegas_odds")
class GameVegasOdds(
    @Id
    @Column(name = "game_id")
    var gameId: Int = 0,
    @Basic
    @Column(name = "home_vegas_spread")
    var homeVegasSpread: Double? = null,
    @Basic
    @Column(name = "away_vegas_spread")
    var awayVegasSpread: Double? = null,
)
