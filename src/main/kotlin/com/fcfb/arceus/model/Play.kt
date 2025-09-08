package com.fcfb.arceus.model

import com.fcfb.arceus.enums.play.ActualResult
import com.fcfb.arceus.enums.play.PlayCall
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.TeamSide
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
@Table(name = "play")
class Play {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "play_id")
    var playId: Int = 0

    @Basic
    @Column(name = "game_id")
    var gameId: Int = 0

    @Basic
    @Column(name = "play_number")
    var playNumber: Int = 0

    @Basic
    @Column(name = "home_score")
    var homeScore: Int = 0

    @Basic
    @Column(name = "away_score")
    var awayScore: Int = 0

    @Basic
    @Column(name = "quarter")
    var quarter: Int = 1

    @Basic
    @Column(name = "clock")
    var clock: Int = 420

    @Basic
    @Column(name = "ball_location")
    var ballLocation: Int = 0

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "possession")
    var possession: TeamSide = TeamSide.HOME

    @Basic
    @Column(name = "down")
    var down: Int = 1

    @Basic
    @Column(name = "yards_to_go")
    var yardsToGo: Int = 100

    @Basic
    @Column(name = "defensive_number")
    var defensiveNumber: String? = null

    @Basic
    @Column(name = "offensive_number")
    var offensiveNumber: String? = null

    @Basic
    @Column(name = "defensive_submitter")
    var defensiveSubmitter: String? = null

    @Basic
    @Column(name = "defensive_submitter_id")
    var defensiveSubmitterId: String? = null

    @Basic
    @Column(name = "offensive_submitter")
    var offensiveSubmitter: String? = null

    @Basic
    @Column(name = "offensive_submitter_id")
    var offensiveSubmitterId: String? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "play_call")
    var playCall: PlayCall? = null

    @Basic
    @Column(name = "result")
    var result: Scenario? = null

    @Basic
    @Column(name = "difference")
    var difference: Int? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "actual_result")
    var actualResult: ActualResult? = null

    @Basic
    @Column(name = "yards")
    var yards: Int = 0

    @Basic
    @Column(name = "play_time")
    var playTime: Int = 0

    @Basic
    @Column(name = "runoff_time")
    var runoffTime: Int = 0

    @Basic
    @Column(name = "win_probability")
    var winProbability: Double? = null

    @Basic
    @Column(name = "win_probability_added")
    var winProbabilityAdded: Double? = null

    @Basic
    @Column(name = "home_team")
    lateinit var homeTeam: String

    @Basic
    @Column(name = "away_team")
    lateinit var awayTeam: String

    @Basic
    @Column(name = "timeout_used")
    var timeoutUsed: Boolean = false

    @Basic
    @Column(name = "offensive_timeout_called")
    var offensiveTimeoutCalled: Boolean = false

    @Basic
    @Column(name = "defensive_timeout_called")
    var defensiveTimeoutCalled: Boolean = false

    @Basic
    @Column(name = "home_timeouts")
    var homeTimeouts: Int = 3

    @Basic
    @Column(name = "away_timeouts")
    var awayTimeouts: Int = 3

    @Basic
    @Column(name = "play_finished")
    var playFinished: Boolean = false

    @Basic
    @Column(name = "offensive_response_speed")
    var offensiveResponseSpeed: Long? = null

    @Basic
    @Column(name = "defensive_response_speed")
    var defensiveResponseSpeed: Long? = null

    constructor(
        gameId: Int,
        playNumber: Int,
        homeScore: Int,
        awayScore: Int,
        quarter: Int,
        clock: Int,
        ballLocation: Int,
        possession: TeamSide,
        down: Int,
        yardsToGo: Int,
        defensiveNumber: String?,
        offensiveNumber: String?,
        offensiveSubmitter: String?,
        offensiveSubmitterId: String?,
        defensiveSubmitter: String?,
        defensiveSubmitterId: String?,
        playCall: PlayCall?,
        result: Scenario?,
        actualResult: ActualResult?,
        yards: Int,
        playTime: Int,
        runoffTime: Int,
        winProbability: Double?,
        winProbabilityAdded: Double,
        homeTeam: String,
        awayTeam: String,
        difference: Int,
        timeoutUsed: Boolean,
        offensiveTimeoutCalled: Boolean,
        defensiveTimeoutCalled: Boolean,
        homeTimeouts: Int,
        awayTimeouts: Int,
        playFinished: Boolean,
        offensiveResponseSpeed: Long?,
        defensiveResponseSpeed: Long?,
    ) {
        this.gameId = gameId
        this.playNumber = playNumber
        this.homeScore = homeScore
        this.awayScore = awayScore
        this.quarter = quarter
        this.clock = clock
        this.ballLocation = ballLocation
        this.possession = possession
        this.down = down
        this.yardsToGo = yardsToGo
        this.defensiveNumber = defensiveNumber
        this.offensiveNumber = offensiveNumber
        this.offensiveSubmitter = offensiveSubmitter
        this.offensiveSubmitterId = offensiveSubmitterId
        this.defensiveSubmitter = defensiveSubmitter
        this.defensiveSubmitterId = defensiveSubmitterId
        this.playCall = playCall
        this.result = result
        this.actualResult = actualResult
        this.yards = yards
        this.playTime = playTime
        this.runoffTime = runoffTime
        this.winProbability = winProbability
        this.winProbabilityAdded = winProbabilityAdded
        this.homeTeam = homeTeam
        this.awayTeam = awayTeam
        this.difference = difference
        this.timeoutUsed = timeoutUsed
        this.offensiveTimeoutCalled = offensiveTimeoutCalled
        this.defensiveTimeoutCalled = defensiveTimeoutCalled
        this.homeTimeouts = homeTimeouts
        this.awayTimeouts = awayTimeouts
        this.playFinished = playFinished
        this.offensiveResponseSpeed = offensiveResponseSpeed
        this.defensiveResponseSpeed = defensiveResponseSpeed
    }

    constructor()
}
