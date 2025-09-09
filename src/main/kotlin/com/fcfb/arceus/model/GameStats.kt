package com.fcfb.arceus.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fcfb.arceus.enums.game.GameStatus
import com.fcfb.arceus.enums.game.GameType
import com.fcfb.arceus.enums.game.TVChannel
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import org.hibernate.annotations.Type
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
@Table(name = "game_stats")
class GameStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int = 0,
    @Basic
    @Column(name = "game_id")
    var gameId: Int = 0,
    @Basic
    @Column(name = "team")
    var team: String? = null,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "tv_channel")
    var tvChannel: TVChannel? = null,
    @Type(type = "json")
    @Column(name = "coaches", columnDefinition = "json")
    @JsonProperty("coaches")
    var coaches: List<String>? = listOf(),
    @Basic
    @Column(name = "offensive_playbook")
    var offensivePlaybook: OffensivePlaybook? = null,
    @Basic
    @Column(name = "defensive_playbook")
    var defensivePlaybook: DefensivePlaybook? = null,
    @Basic
    @Column(name = "season")
    var season: Int? = null,
    @Basic
    @Column(name = "week")
    var week: Int? = null,
    @Basic
    @Column(name = "subdivision")
    var subdivision: Subdivision? = null,
    @Basic
    @Column(name = "score")
    var score: Int = 0,
    @Basic
    @Column(name = "pass_attempts")
    var passAttempts: Int = 0,
    @Basic
    @Column(name = "pass_completions")
    var passCompletions: Int = 0,
    @Basic
    @Column(name = "pass_completion_percentage")
    var passCompletionPercentage: Double = 0.0,
    @Basic
    @Column(name = "pass_yards")
    var passYards: Int = 0,
    @Basic
    @Column(name = "longest_pass")
    var longestPass: Int = 0,
    @Basic
    @Column(name = "sacks_allowed")
    var sacksAllowed: Int = 0,
    @Basic
    @Column(name = "sacks_forced")
    var sacksForced: Int = 0,
    @Basic
    @Column(name = "rush_attempts")
    var rushAttempts: Int = 0,
    @Basic
    @Column(name = "rush_successes")
    var rushSuccesses: Int = 0,
    @Basic
    @Column(name = "rush_success_percentage")
    var rushSuccessPercentage: Double = 0.0,
    @Basic
    @Column(name = "pass_successes")
    var passSuccesses: Int = 0,
    @Basic
    @Column(name = "pass_success_percentage")
    var passSuccessPercentage: Double = 0.0,
    @Basic
    @Column(name = "rush_yards")
    var rushYards: Int = 0,
    @Basic
    @Column(name = "longest_run")
    var longestRun: Int = 0,
    @Basic
    @Column(name = "total_yards")
    var totalYards: Int = 0,
    @Basic
    @Column(name = "interceptions_lost")
    var interceptionsLost: Int = 0,
    @Basic
    @Column(name = "interceptions_forced")
    var interceptionsForced: Int = 0,
    @Basic
    @Column(name = "fumbles_lost")
    var fumblesLost: Int = 0,
    @Basic
    @Column(name = "fumbles_forced")
    var fumblesForced: Int = 0,
    @Basic
    @Column(name = "turnovers_lost")
    var turnoversLost: Int = 0,
    @Basic
    @Column(name = "turnovers_forced")
    var turnoversForced: Int = 0,
    @Basic
    @Column(name = "turnover_touchdowns_lost")
    var turnoverTouchdownsLost: Int = 0,
    @Basic
    @Column(name = "turnover_touchdowns_forced")
    var turnoverTouchdownsForced: Int = 0,
    @Basic
    @Column(name = "field_goal_made")
    var fieldGoalMade: Int = 0,
    @Basic
    @Column(name = "field_goal_attempts")
    var fieldGoalAttempts: Int = 0,
    @Basic
    @Column(name = "field_goal_percentage")
    var fieldGoalPercentage: Double = 0.0,
    @Basic
    @Column(name = "longest_field_goal")
    var longestFieldGoal: Int = 0,
    @Basic
    @Column(name = "blocked_opponent_field_goals")
    var blockedOpponentFieldGoals: Int = 0,
    @Basic
    @Column(name = "field_goal_touchdown")
    var fieldGoalTouchdown: Int = 0,
    @Basic
    @Column(name = "punts_attempted")
    var puntsAttempted: Int = 0,
    @Basic
    @Column(name = "longest_punt")
    var longestPunt: Int = 0,
    @Basic
    @Column(name = "average_punt_length")
    var averagePuntLength: Double = 0.0,
    @Basic
    @Column(name = "blocked_opponent_punt")
    var blockedOpponentPunt: Int = 0,
    @Basic
    @Column(name = "punt_return_td")
    var puntReturnTd: Int = 0,
    @Basic
    @Column(name = "punt_return_td_percentage")
    var puntReturnTdPercentage: Double = 0.0,
    @Basic
    @Column(name = "number_of_kickoffs")
    var numberOfKickoffs: Int = 0,
    @Basic
    @Column(name = "onside_attempts")
    var onsideAttempts: Int = 0,
    @Basic
    @Column(name = "onside_success")
    var onsideSuccess: Int = 0,
    @Basic
    @Column(name = "onside_success_percentage")
    var onsideSuccessPercentage: Double = 0.0,
    @Basic
    @Column(name = "normal_kickoff_attempts")
    var normalKickoffAttempts: Int = 0,
    @Basic
    @Column(name = "touchbacks")
    var touchbacks: Int = 0,
    @Basic
    @Column(name = "touchback_percentage")
    var touchbackPercentage: Double = 0.0,
    @Basic
    @Column(name = "kick_return_td")
    var kickReturnTd: Int = 0,
    @Basic
    @Column(name = "kick_return_td_percentage")
    var kickReturnTdPercentage: Double = 0.0,
    @Basic
    @Column(name = "number_of_drives")
    var numberOfDrives: Int = 0,
    @Basic
    @Column(name = "time_of_possession")
    var timeOfPossession: Int = 0,
    @Basic
    @Column(name = "q1_score")
    var q1Score: Int = 0,
    @Basic
    @Column(name = "q2_score")
    var q2Score: Int = 0,
    @Basic
    @Column(name = "q3_score")
    var q3Score: Int = 0,
    @Basic
    @Column(name = "q4_score")
    var q4Score: Int = 0,
    @Basic
    @Column(name = "ot_score")
    var otScore: Int = 0,
    @Basic
    @Column(name = "touchdowns")
    var touchdowns: Int = 0,
    @Basic
    @Column(name = "average_offensive_diff")
    var averageOffensiveDiff: Double = 0.0,
    @Basic
    @Column(name = "average_defensive_diff")
    var averageDefensiveDiff: Double = 0.0,
    @Basic
    @Column(name = "average_offensive_special_teams_diff")
    var averageOffensiveSpecialTeamsDiff: Double = 0.0,
    @Basic
    @Column(name = "average_defensive_special_teams_diff")
    var averageDefensiveSpecialTeamsDiff: Double = 0.0,
    @Basic
    @Column(name = "average_yards_per_play")
    var averageYardsPerPlay: Double = 0.0,
    @Basic
    @Column(name = "first_downs")
    var firstDowns: Int = 0,
    @Basic
    @Column(name = "third_down_conversion_success")
    var thirdDownConversionSuccess: Int = 0,
    @Basic
    @Column(name = "third_down_conversion_attempts")
    var thirdDownConversionAttempts: Int = 0,
    @Basic
    @Column(name = "third_down_conversion_percentage")
    var thirdDownConversionPercentage: Double = 0.0,
    @Basic
    @Column(name = "fourth_down_conversion_success")
    var fourthDownConversionSuccess: Int = 0,
    @Basic
    @Column(name = "fourth_down_conversion_attempts")
    var fourthDownConversionAttempts: Int = 0,
    @Basic
    @Column(name = "fourth_down_conversion_percentage")
    var fourthDownConversionPercentage: Double = 0.0,
    @Basic
    @Column(name = "largest_lead")
    var largestLead: Int = 0,
    @Basic
    @Column(name = "largest_deficit")
    var largestDeficit: Int = 0,
    @Basic
    @Column(name = "pass_touchdowns")
    var passTouchdowns: Int = 0,
    @Basic
    @Column(name = "rush_touchdowns")
    var rushTouchdowns: Int = 0,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "game_type")
    var gameType: GameType? = GameType.SCRIMMAGE,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "game_status")
    var gameStatus: GameStatus? = null,
    @Basic
    @Column(name = "red_zone_attempts")
    var redZoneAttempts: Int = 0,
    @Basic
    @Column(name = "red_zone_successes")
    var redZoneSuccesses: Int = 0,
    @Basic
    @Column(name = "red_zone_success_percentage")
    var redZoneSuccessPercentage: Double = 0.0,
    @Basic
    @Column(name = "red_zone_percentage")
    var redZonePercentage: Double = 0.0,
    @Basic
    @Column(name = "average_diff")
    var averageDiff: Double? = 0.9,
    @Basic
    @Column(name = "turnover_differential")
    var turnoverDifferential: Int = 0,
    @Basic
    @Column(name = "pick_sixes_thrown")
    var pickSixesThrown: Int = 0,
    @Basic
    @Column(name = "pick_sixes_forced")
    var pickSixesForced: Int = 0,
    @Basic
    @Column(name = "fumble_return_tds_committed")
    var fumbleReturnTdsCommitted: Int = 0,
    @Basic
    @Column(name = "fumble_return_tds_forced")
    var fumbleReturnTdsForced: Int = 0,
    @Basic
    @Column(name = "safeties_forced")
    var safetiesForced: Int = 0,
    @Basic
    @Column(name = "safeties_committed")
    var safetiesCommitted: Int = 0,
    @Basic
    @Column(name = "average_response_speed")
    var averageResponseSpeed: Double = 0.0,
    @Basic
    @Column(name = "last_modified_ts")
    var lastModifiedTs: String? = null,
    @Basic
    @Column(name = "team_elo")
    var teamElo: Double = 1500.0,
)
