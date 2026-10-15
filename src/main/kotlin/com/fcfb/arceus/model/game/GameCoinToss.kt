package com.fcfb.arceus.model.game

import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.arceus.enums.team.TeamSide
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "game_coin_toss")
class GameCoinToss(
    @Id
    @Column(name = "game_id")
    var gameId: Int = 0,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "coin_toss_winner")
    var coinTossWinner: TeamSide? = null,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "coin_toss_choice")
    var coinTossChoice: CoinTossChoice? = null,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "overtime_coin_toss_winner")
    var overtimeCoinTossWinner: TeamSide? = null,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "overtime_coin_toss_choice")
    var overtimeCoinTossChoice: OvertimeCoinTossChoice? = null,
)
