package com.fcfb.arceus.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.GameWarning
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.gameflow.CoinTossChoice
import com.fcfb.arceus.enums.gameflow.OvertimeCoinTossChoice
import com.fcfb.arceus.enums.play.PlayType
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.enums.team.TeamSide
import com.vladmihalcea.hibernate.type.json.JsonStringType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
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
@Table(name = "game")
@TypeDef(name = "json", typeClass = JsonStringType::class)
class Game {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "game_id")
    @JsonProperty("game_id")
    var gameId: Int = 0

    @Basic
    @Column(name = "home_team")
    @JsonProperty("home_team")
    lateinit var homeTeam: String

    @Basic
    @Column(name = "away_team")
    @JsonProperty("away_team")
    lateinit var awayTeam: String

    @Type(type = "json")
    @Column(name = "home_coaches", columnDefinition = "json")
    @JsonProperty("home_coaches")
    var homeCoaches: List<String>? = listOf()

    @Type(type = "json")
    @Column(name = "away_coaches", columnDefinition = "json")
    @JsonProperty("away_coaches")
    var awayCoaches: List<String>? = listOf()

    @Type(type = "json")
    @Column(name = "home_coach_discord_ids")
    @JsonProperty("home_coach_discord_ids")
    var homeCoachDiscordIds: List<String>? = listOf()

    @Type(type = "json")
    @Column(name = "away_coach_discord_ids")
    @JsonProperty("away_coach_discord_ids")
    var awayCoachDiscordIds: List<String>? = listOf()

    @Basic
    @Column(name = "home_offensive_playbook")
    @JsonProperty("home_offensive_playbook")
    lateinit var homeOffensivePlaybook: OffensivePlaybook

    @Basic
    @Column(name = "away_offensive_playbook")
    @JsonProperty("away_offensive_playbook")
    lateinit var awayOffensivePlaybook: OffensivePlaybook

    @Basic
    @Column(name = "home_defensive_playbook")
    @JsonProperty("home_defensive_playbook")
    lateinit var homeDefensivePlaybook: DefensivePlaybook

    @Basic
    @Column(name = "away_defensive_playbook")
    @JsonProperty("away_defensive_playbook")
    lateinit var awayDefensivePlaybook: DefensivePlaybook

    @Basic
    @Column(name = "home_score")
    @JsonProperty("home_score")
    var homeScore: Int = 0

    @Basic
    @Column(name = "away_score")
    @JsonProperty("away_score")
    var awayScore: Int = 0

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "possession")
    @JsonProperty("possession")
    var possession: TeamSide = TeamSide.HOME

    @Basic
    @Column(name = "quarter")
    @JsonProperty("quarter")
    var quarter: Int = 1

    @Basic
    @Column(name = "clock")
    @JsonProperty("clock")
    var clock: String = "7:00"

    @Basic
    @Column(name = "ball_location")
    @JsonProperty("ball_location")
    var ballLocation: Int = 0

    @Basic
    @Column(name = "down")
    @JsonProperty("down")
    var down: Int = 1

    @Basic
    @Column(name = "yards_to_go")
    @JsonProperty("yards_to_go")
    var yardsToGo: Int = 10

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "tv_channel")
    @JsonProperty("tv_channel")
    var tvChannel: TVChannel? = null

    @Basic
    @Column(name = "home_team_rank")
    @JsonProperty("home_team_rank")
    var homeTeamRank: Int? = 0

    @Basic
    @Column(name = "home_wins")
    @JsonProperty("home_wins")
    var homeWins: Int? = null

    @Basic
    @Column(name = "home_losses")
    @JsonProperty("home_losses")
    var homeLosses: Int? = null

    @Basic
    @Column(name = "away_wins")
    @JsonProperty("away_wins")
    var awayWins: Int? = null

    @Basic
    @Column(name = "away_losses")
    @JsonProperty("away_losses")
    var awayLosses: Int? = null

    @Basic
    @Column(name = "away_team_rank")
    @JsonProperty("away_team_rank")
    var awayTeamRank: Int? = 0

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "subdivision")
    @JsonProperty("subdivision")
    var subdivision: Subdivision? = null

    @Basic
    @Column(name = "timestamp")
    @JsonProperty("timestamp")
    var timestamp: String? = null

    @Basic
    @Column(name = "win_probability")
    @JsonProperty("win_probability")
    var winProbability: Double? = null

    @Basic
    @Column(name = "season")
    @JsonProperty("season")
    var season: Int? = null

    @Basic
    @Column(name = "week")
    @JsonProperty("week")
    var week: Int? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "waiting_on")
    @JsonProperty("waiting_on")
    var waitingOn: TeamSide = TeamSide.AWAY

    @Basic
    @Column(name = "num_plays")
    @JsonProperty("num_plays")
    var numPlays: Int = 0

    @Basic
    @Column(name = "home_timeouts")
    @JsonProperty("home_timeouts")
    var homeTimeouts: Int = 3

    @Basic
    @Column(name = "away_timeouts")
    @JsonProperty("away_timeouts")
    var awayTimeouts: Int = 3

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "coin_toss_winner")
    @JsonProperty("coin_toss_winner")
    var coinTossWinner: TeamSide? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "coin_toss_choice")
    @JsonProperty("coin_toss_choice")
    var coinTossChoice: CoinTossChoice? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "overtime_coin_toss_winner")
    @JsonProperty("overtime_coin_toss_winner")
    var overtimeCoinTossWinner: TeamSide? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "overtime_coin_toss_choice")
    @JsonProperty("overtime_coin_toss_choice")
    var overtimeCoinTossChoice: OvertimeCoinTossChoice? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "home_platform")
    @JsonProperty("home_platform")
    lateinit var homePlatform: com.fcfb.arceus.enums.system.Platform

    @Basic
    @Column(name = "home_platform_id")
    @JsonProperty("home_platform_id")
    var homePlatformId: String? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "away_platform")
    @JsonProperty("away_platform")
    lateinit var awayPlatform: com.fcfb.arceus.enums.system.Platform

    @Basic
    @Column(name = "away_platform_id")
    @JsonProperty("away_platform_id")
    var awayPlatformId: String? = null

    @Basic
    @Column(name = "last_message_timestamp")
    @JsonProperty("last_message_timestamp")
    var lastMessageTimestamp: String? = null

    @Basic
    @Column(name = "game_timer")
    @JsonProperty("game_timer")
    var gameTimer: String? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "game_warning")
    @JsonProperty("game_warning")
    var gameWarning: GameWarning? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "current_play_type")
    @JsonProperty("current_play_type")
    var currentPlayType: PlayType? = null

    @Basic
    @Column(name = "current_play_id")
    @JsonProperty("current_play_id")
    var currentPlayId: Int? = null

    @Basic
    @Column(name = "clock_stopped")
    @JsonProperty("clock_stopped")
    var clockStopped: Boolean = false

    @Type(type = "json")
    @Column(name = "request_message_id", columnDefinition = "json")
    @JsonProperty("request_message_id")
    var requestMessageId: List<String>? = listOf()

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "game_status")
    @JsonProperty("game_status")
    var gameStatus: GameStatus? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "game_type")
    @JsonProperty("game_type")
    var gameType: GameType? = null

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "game_mode")
    @JsonProperty("game_mode")
    var gameMode: GameMode? = null

    @Basic
    @Column(name = "overtime_half")
    @JsonProperty("overtime_half")
    var overtimeHalf: Int? = 0

    @Basic
    @Column(name = "close_game")
    @JsonProperty("close_game")
    var closeGame: Boolean = false

    @Basic
    @Column(name = "close_game_pinged")
    @JsonProperty("close_game_pinged")
    var closeGamePinged: Boolean = false

    @Basic
    @Column(name = "upset_alert")
    @JsonProperty("upset_alert")
    var upsetAlert: Boolean = false

    @Basic
    @Column(name = "upset_alert_pinged")
    @JsonProperty("upset_alert_pinged")
    var upsetAlertPinged: Boolean = false

    @Basic
    @Column(name = "home_vegas_spread")
    @JsonProperty("home_vegas_spread")
    var homeVegasSpread: Double? = null

    @Basic
    @Column(name = "away_vegas_spread")
    @JsonProperty("away_vegas_spread")
    var awayVegasSpread: Double? = null

    @Basic
    @Column(name = "postseason_game_logo")
    @JsonProperty("postseason_game_logo")
    var postseasonGameLogo: String? = null

    constructor(
        homeTeam: String,
        awayTeam: String,
        homeCoaches: List<String>,
        awayCoaches: List<String>,
        homeCoachDiscordIds: List<String>,
        awayCoachDiscordIds: List<String>,
        homeOffensivePlaybook: OffensivePlaybook,
        awayOffensivePlaybook: OffensivePlaybook,
        homeDefensivePlaybook: DefensivePlaybook,
        awayDefensivePlaybook: DefensivePlaybook,
        homeScore: Int,
        awayScore: Int,
        possession: TeamSide,
        quarter: Int,
        clock: String,
        ballLocation: Int,
        down: Int,
        yardsToGo: Int,
        tvChannel: TVChannel?,
        homeTeamRank: Int?,
        homeWins: Int?,
        homeLosses: Int?,
        awayTeamRank: Int?,
        awayWins: Int?,
        awayLosses: Int?,
        subdivision: Subdivision?,
        timestamp: String?,
        winProbability: Double?,
        season: Int?,
        week: Int?,
        waitingOn: TeamSide,
        numPlays: Int,
        homeTimeouts: Int,
        awayTimeouts: Int,
        coinTossWinner: TeamSide?,
        coinTossChoice: CoinTossChoice?,
        overtimeCoinTossWinner: TeamSide?,
        overtimeCoinTossChoice: OvertimeCoinTossChoice?,
        homePlatform: com.fcfb.arceus.enums.system.Platform,
        homePlatformId: String?,
        awayPlatform: com.fcfb.arceus.enums.system.Platform,
        awayPlatformId: String?,
        lastMessageTimestamp: String?,
        gameTimer: String?,
        gameWarning: GameWarning?,
        currentPlayType: PlayType?,
        currentPlayId: Int?,
        clockStopped: Boolean,
        requestMessageId: List<String>?,
        gameStatus: GameStatus?,
        gameType: GameType?,
        gameMode: GameMode?,
        overtimeHalf: Int?,
        closeGame: Boolean,
        closeGamePinged: Boolean,
        upsetAlert: Boolean,
        upsetAlertPinged: Boolean,
        homeVegasSpread: Double? = null,
        awayVegasSpread: Double? = null,
        postseasonGameLogo: String? = null,
    ) {
        this.homeTeam = homeTeam
        this.awayTeam = awayTeam
        this.homeCoaches = homeCoaches
        this.awayCoaches = awayCoaches
        this.homeCoachDiscordIds = homeCoachDiscordIds
        this.awayCoachDiscordIds = awayCoachDiscordIds
        this.homeOffensivePlaybook = homeOffensivePlaybook
        this.awayOffensivePlaybook = awayOffensivePlaybook
        this.homeDefensivePlaybook = homeDefensivePlaybook
        this.awayDefensivePlaybook = awayDefensivePlaybook
        this.homeScore = homeScore
        this.awayScore = awayScore
        this.possession = possession
        this.quarter = quarter
        this.clock = clock
        this.ballLocation = ballLocation
        this.down = down
        this.yardsToGo = yardsToGo
        this.tvChannel = tvChannel
        this.homeTeamRank = homeTeamRank
        this.homeWins = homeWins
        this.homeLosses = homeLosses
        this.awayTeamRank = awayTeamRank
        this.awayWins = awayWins
        this.awayLosses = awayLosses
        this.subdivision = subdivision
        this.timestamp = timestamp
        this.winProbability = winProbability
        this.season = season
        this.week = week
        this.waitingOn = waitingOn
        this.numPlays = numPlays
        this.homeTimeouts = homeTimeouts
        this.awayTimeouts = awayTimeouts
        this.coinTossWinner = coinTossWinner
        this.coinTossChoice = coinTossChoice
        this.overtimeCoinTossWinner = overtimeCoinTossWinner
        this.overtimeCoinTossChoice = overtimeCoinTossChoice
        this.homePlatform = homePlatform
        this.homePlatformId = homePlatformId
        this.awayPlatform = awayPlatform
        this.awayPlatformId = awayPlatformId
        this.lastMessageTimestamp = lastMessageTimestamp
        this.gameTimer = gameTimer
        this.gameWarning = gameWarning
        this.currentPlayType = currentPlayType
        this.currentPlayId = currentPlayId
        this.clockStopped = clockStopped
        this.requestMessageId = requestMessageId
        this.gameStatus = gameStatus
        this.gameType = gameType
        this.gameMode = gameMode
        this.overtimeHalf = overtimeHalf
        this.closeGame = closeGame
        this.closeGamePinged = closeGamePinged
        this.upsetAlert = upsetAlert
        this.upsetAlertPinged = upsetAlertPinged
        this.homeVegasSpread = homeVegasSpread
        this.awayVegasSpread = awayVegasSpread
        this.postseasonGameLogo = postseasonGameLogo
    }

    constructor()
}
