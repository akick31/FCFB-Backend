package com.fcfb.arceus.model

import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.enums.team.Subdivision
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
@Table(name = "season_stats")
class SeasonStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int = 0,
    @Basic
    @Column(name = "team", nullable = false)
    var team: String,
    @Basic
    @Column(name = "season_number", nullable = false)
    var seasonNumber: Int,
    @Basic
    @Column(name = "wins")
    var wins: Int = 0,
    @Basic
    @Column(name = "losses")
    var losses: Int = 0,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "subdivision")
    var subdivision: Subdivision? = null,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "conference")
    var conference: Conference? = null,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "offensive_playbook")
    var offensivePlaybook: com.fcfb.arceus.enums.team.OffensivePlaybook? = null,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "defensive_playbook")
    var defensivePlaybook: com.fcfb.arceus.enums.team.DefensivePlaybook? = null,
    // Passing Stats (Season Totals)
    @Basic
    @Column(name = "pass_attempts")
    var passAttempts: Int = 0,
    @Basic
    @Column(name = "pass_completions")
    var passCompletions: Int = 0,
    @Basic
    @Column(name = "pass_completion_percentage")
    var passCompletionPercentage: Double? = null,
    @Basic
    @Column(name = "pass_yards")
    var passYards: Int = 0,
    @Basic
    @Column(name = "longest_pass")
    var longestPass: Int = 0,
    @Basic
    @Column(name = "pass_touchdowns")
    var passTouchdowns: Int = 0,
    @Basic
    @Column(name = "pass_successes")
    var passSuccesses: Int = 0,
    @Basic
    @Column(name = "pass_success_percentage")
    var passSuccessPercentage: Double? = null,
    // Rushing Stats (Season Totals)
    @Basic
    @Column(name = "rush_attempts")
    var rushAttempts: Int = 0,
    @Basic
    @Column(name = "rush_successes")
    var rushSuccesses: Int = 0,
    @Basic
    @Column(name = "rush_success_percentage")
    var rushSuccessPercentage: Double? = null,
    @Basic
    @Column(name = "rush_yards")
    var rushYards: Int = 0,
    @Basic
    @Column(name = "longest_run")
    var longestRun: Int = 0,
    @Basic
    @Column(name = "rush_touchdowns")
    var rushTouchdowns: Int = 0,
    // Total Offense (Season Totals)
    @Basic
    @Column(name = "total_yards")
    var totalYards: Int = 0,
    @Basic
    @Column(name = "average_yards_per_play")
    var averageYardsPerPlay: Double? = null,
    @Basic
    @Column(name = "first_downs")
    var firstDowns: Int = 0,
    // Sacks (Season Totals)
    @Basic
    @Column(name = "sacks_allowed")
    var sacksAllowed: Int = 0,
    @Basic
    @Column(name = "sacks_forced")
    var sacksForced: Int = 0,
    // Turnovers (Season Totals)
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
    @Column(name = "turnover_differential")
    var turnoverDifferential: Int = 0,
    @Basic
    @Column(name = "turnover_touchdowns_lost")
    var turnoverTouchdownsLost: Int = 0,
    @Basic
    @Column(name = "turnover_touchdowns_forced")
    var turnoverTouchdownsForced: Int = 0,
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
    // Field Goals (Season Totals)
    @Basic
    @Column(name = "field_goal_made")
    var fieldGoalMade: Int = 0,
    @Basic
    @Column(name = "field_goal_attempts")
    var fieldGoalAttempts: Int = 0,
    @Basic
    @Column(name = "field_goal_percentage")
    var fieldGoalPercentage: Double? = null,
    @Basic
    @Column(name = "longest_field_goal")
    var longestFieldGoal: Int = 0,
    @Basic
    @Column(name = "blocked_opponent_field_goals")
    var blockedOpponentFieldGoals: Int = 0,
    @Basic
    @Column(name = "field_goal_touchdown")
    var fieldGoalTouchdown: Int = 0,
    // Punting (Season Totals)
    @Basic
    @Column(name = "punts_attempted")
    var puntsAttempted: Int = 0,
    @Basic
    @Column(name = "longest_punt")
    var longestPunt: Int = 0,
    @Basic
    @Column(name = "average_punt_length")
    var averagePuntLength: Double? = null,
    @Basic
    @Column(name = "blocked_opponent_punt")
    var blockedOpponentPunt: Int = 0,
    @Basic
    @Column(name = "punt_return_td")
    var puntReturnTd: Int = 0,
    @Basic
    @Column(name = "punt_return_td_percentage")
    var puntReturnTdPercentage: Double? = null,
    // Kickoffs (Season Totals)
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
    var onsideSuccessPercentage: Double? = null,
    @Basic
    @Column(name = "normal_kickoff_attempts")
    var normalKickoffAttempts: Int = 0,
    @Basic
    @Column(name = "touchbacks")
    var touchbacks: Int = 0,
    @Basic
    @Column(name = "touchback_percentage")
    var touchbackPercentage: Double? = null,
    @Basic
    @Column(name = "kick_return_td")
    var kickReturnTd: Int = 0,
    @Basic
    @Column(name = "kick_return_td_percentage")
    var kickReturnTdPercentage: Double? = null,
    // Game Flow (Season Totals)
    @Basic
    @Column(name = "number_of_drives")
    var numberOfDrives: Int = 0,
    @Basic
    @Column(name = "time_of_possession")
    var timeOfPossession: Int = 0,
    // Touchdowns (Season Totals)
    @Basic
    @Column(name = "touchdowns")
    var touchdowns: Int = 0,
    // Down Conversions (Season Totals)
    @Basic
    @Column(name = "third_down_conversion_success")
    var thirdDownConversionSuccess: Int = 0,
    @Basic
    @Column(name = "third_down_conversion_attempts")
    var thirdDownConversionAttempts: Int = 0,
    @Basic
    @Column(name = "third_down_conversion_percentage")
    var thirdDownConversionPercentage: Double? = null,
    @Basic
    @Column(name = "fourth_down_conversion_success")
    var fourthDownConversionSuccess: Int = 0,
    @Basic
    @Column(name = "fourth_down_conversion_attempts")
    var fourthDownConversionAttempts: Int = 0,
    @Basic
    @Column(name = "fourth_down_conversion_percentage")
    var fourthDownConversionPercentage: Double? = null,
    // Game Control (Season Totals)
    @Basic
    @Column(name = "largest_lead")
    var largestLead: Int = 0,
    @Basic
    @Column(name = "largest_deficit")
    var largestDeficit: Int = 0,
    // Red Zone (Season Totals)
    @Basic
    @Column(name = "red_zone_attempts")
    var redZoneAttempts: Int = 0,
    @Basic
    @Column(name = "red_zone_successes")
    var redZoneSuccesses: Int = 0,
    @Basic
    @Column(name = "red_zone_success_percentage")
    var redZoneSuccessPercentage: Double? = null,
    @Basic
    @Column(name = "red_zone_percentage")
    var redZonePercentage: Double? = null,
    // Special Teams (Season Totals)
    @Basic
    @Column(name = "safeties_forced")
    var safetiesForced: Int = 0,
    @Basic
    @Column(name = "safeties_committed")
    var safetiesCommitted: Int = 0,
    // Performance Metrics (Season Averages)
    @Basic
    @Column(name = "average_offensive_diff")
    var averageOffensiveDiff: Double? = null,
    @Basic
    @Column(name = "average_defensive_diff")
    var averageDefensiveDiff: Double? = null,
    @Basic
    @Column(name = "average_offensive_special_teams_diff")
    var averageOffensiveSpecialTeamsDiff: Double? = null,
    @Basic
    @Column(name = "average_defensive_special_teams_diff")
    var averageDefensiveSpecialTeamsDiff: Double? = null,
    @Basic
    @Column(name = "average_diff")
    var averageDiff: Double? = null,
    @Basic
    @Column(name = "average_response_speed")
    var averageResponseSpeed: Double? = null,
    // Opponent Stats (what the team allowed opponents to do)
    // Opponent Passing Stats (Season Totals)
    @Basic
    @Column(name = "opponent_pass_attempts")
    var opponentPassAttempts: Int = 0,
    @Basic
    @Column(name = "opponent_pass_completions")
    var opponentPassCompletions: Int = 0,
    @Basic
    @Column(name = "opponent_pass_completion_percentage")
    var opponentPassCompletionPercentage: Double? = null,
    @Basic
    @Column(name = "opponent_pass_yards")
    var opponentPassYards: Int = 0,
    @Basic
    @Column(name = "opponent_longest_pass")
    var opponentLongestPass: Int = 0,
    @Basic
    @Column(name = "opponent_pass_touchdowns")
    var opponentPassTouchdowns: Int = 0,
    @Basic
    @Column(name = "opponent_pass_successes")
    var opponentPassSuccesses: Int = 0,
    @Basic
    @Column(name = "opponent_pass_success_percentage")
    var opponentPassSuccessPercentage: Double? = null,
    // Opponent Rushing Stats (Season Totals)
    @Basic
    @Column(name = "opponent_rush_attempts")
    var opponentRushAttempts: Int = 0,
    @Basic
    @Column(name = "opponent_rush_successes")
    var opponentRushSuccesses: Int = 0,
    @Basic
    @Column(name = "opponent_rush_success_percentage")
    var opponentRushSuccessPercentage: Double? = null,
    @Basic
    @Column(name = "opponent_rush_yards")
    var opponentRushYards: Int = 0,
    @Basic
    @Column(name = "opponent_longest_run")
    var opponentLongestRun: Int = 0,
    @Basic
    @Column(name = "opponent_rush_touchdowns")
    var opponentRushTouchdowns: Int = 0,
    // Opponent Total Offense (Season Totals)
    @Basic
    @Column(name = "opponent_total_yards")
    var opponentTotalYards: Int = 0,
    @Basic
    @Column(name = "opponent_average_yards_per_play")
    var opponentAverageYardsPerPlay: Double? = null,
    @Basic
    @Column(name = "opponent_first_downs")
    var opponentFirstDowns: Int = 0,
    // Opponent Field Goals (Season Totals)
    @Basic
    @Column(name = "opponent_field_goal_made")
    var opponentFieldGoalMade: Int = 0,
    @Basic
    @Column(name = "opponent_field_goal_attempts")
    var opponentFieldGoalAttempts: Int = 0,
    @Basic
    @Column(name = "opponent_field_goal_percentage")
    var opponentFieldGoalPercentage: Double? = null,
    @Basic
    @Column(name = "opponent_longest_field_goal")
    var opponentLongestFieldGoal: Int = 0,
    @Basic
    @Column(name = "opponent_field_goal_touchdown")
    var opponentFieldGoalTouchdown: Int = 0,
    // Opponent Punting (Season Totals)
    @Basic
    @Column(name = "opponent_punts_attempted")
    var opponentPuntsAttempted: Int = 0,
    @Basic
    @Column(name = "opponent_longest_punt")
    var opponentLongestPunt: Int = 0,
    @Basic
    @Column(name = "opponent_average_punt_length")
    var opponentAveragePuntLength: Double? = null,
    @Basic
    @Column(name = "opponent_punt_return_td")
    var opponentPuntReturnTd: Int = 0,
    @Basic
    @Column(name = "opponent_punt_return_td_percentage")
    var opponentPuntReturnTdPercentage: Double? = null,
    // Opponent Kickoffs (Season Totals)
    @Basic
    @Column(name = "opponent_number_of_kickoffs")
    var opponentNumberOfKickoffs: Int = 0,
    @Basic
    @Column(name = "opponent_onside_attempts")
    var opponentOnsideAttempts: Int = 0,
    @Basic
    @Column(name = "opponent_onside_success")
    var opponentOnsideSuccess: Int = 0,
    @Basic
    @Column(name = "opponent_onside_success_percentage")
    var opponentOnsideSuccessPercentage: Double? = null,
    @Basic
    @Column(name = "opponent_normal_kickoff_attempts")
    var opponentNormalKickoffAttempts: Int = 0,
    @Basic
    @Column(name = "opponent_touchbacks")
    var opponentTouchbacks: Int = 0,
    @Basic
    @Column(name = "opponent_touchback_percentage")
    var opponentTouchbackPercentage: Double? = null,
    @Basic
    @Column(name = "opponent_kick_return_td")
    var opponentKickReturnTd: Int = 0,
    @Basic
    @Column(name = "opponent_kick_return_td_percentage")
    var opponentKickReturnTdPercentage: Double? = null,
    // Opponent Game Flow (Season Totals)
    @Basic
    @Column(name = "opponent_number_of_drives")
    var opponentNumberOfDrives: Int = 0,
    @Basic
    @Column(name = "opponent_time_of_possession")
    var opponentTimeOfPossession: Int = 0,
    // Opponent Touchdowns (Season Totals)
    @Basic
    @Column(name = "opponent_touchdowns")
    var opponentTouchdowns: Int = 0,
    // Opponent Down Conversions (Season Totals)
    @Basic
    @Column(name = "opponent_third_down_conversion_success")
    var opponentThirdDownConversionSuccess: Int = 0,
    @Basic
    @Column(name = "opponent_third_down_conversion_attempts")
    var opponentThirdDownConversionAttempts: Int = 0,
    @Basic
    @Column(name = "opponent_third_down_conversion_percentage")
    var opponentThirdDownConversionPercentage: Double? = null,
    @Basic
    @Column(name = "opponent_fourth_down_conversion_success")
    var opponentFourthDownConversionSuccess: Int = 0,
    @Basic
    @Column(name = "opponent_fourth_down_conversion_attempts")
    var opponentFourthDownConversionAttempts: Int = 0,
    @Basic
    @Column(name = "opponent_fourth_down_conversion_percentage")
    var opponentFourthDownConversionPercentage: Double? = null,
    // Opponent Red Zone (Season Totals)
    @Basic
    @Column(name = "opponent_red_zone_attempts")
    var opponentRedZoneAttempts: Int = 0,
    @Basic
    @Column(name = "opponent_red_zone_successes")
    var opponentRedZoneSuccesses: Int = 0,
    @Basic
    @Column(name = "opponent_red_zone_success_percentage")
    var opponentRedZoneSuccessPercentage: Double? = null,
    @Basic
    @Column(name = "opponent_red_zone_percentage")
    var opponentRedZonePercentage: Double? = null,
    // Additional Season Info
    @Basic
    @Column(name = "last_modified_ts")
    var lastModifiedTs: String? = null,
) {
    constructor() : this(
        team = "",
        seasonNumber = 0,
    )
}
