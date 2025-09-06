package com.fcfb.arceus.model

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
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
@Table(name = "playbook_stats")
class PlaybookStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int = 0,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "offensive_playbook", nullable = false)
    var offensivePlaybook: OffensivePlaybook,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "defensive_playbook", nullable = false)
    var defensivePlaybook: DefensivePlaybook,
    @Basic
    @Column(name = "season_number", nullable = false)
    var seasonNumber: Int,
    @Basic
    @Column(name = "total_teams")
    var totalTeams: Int = 0,
    @Basic
    @Column(name = "total_games")
    var totalGames: Int = 0,
    // Passing Stats (Playbook Totals)
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
    @Column(name = "pass_touchdowns")
    var passTouchdowns: Int = 0,
    @Basic
    @Column(name = "pass_interceptions")
    var passInterceptions: Int = 0,
    @Basic
    @Column(name = "pass_successes")
    var passSuccesses: Int = 0,
    @Basic
    @Column(name = "pass_success_percentage")
    var passSuccessPercentage: Double = 0.0,
    @Basic
    @Column(name = "longest_pass")
    var longestPass: Int = 0,
    // Rushing Stats (Playbook Totals)
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
    @Column(name = "rush_yards")
    var rushYards: Int = 0,
    @Basic
    @Column(name = "rush_touchdowns")
    var rushTouchdowns: Int = 0,
    @Basic
    @Column(name = "longest_run")
    var longestRun: Int = 0,
    // Total Offense
    @Basic
    @Column(name = "total_yards")
    var totalYards: Int = 0,
    @Basic
    @Column(name = "average_yards_per_play")
    var averageYardsPerPlay: Double = 0.0,
    @Basic
    @Column(name = "first_downs")
    var firstDowns: Int = 0,
    // Defense Stats (Playbook Totals)
    @Basic
    @Column(name = "sacks_allowed")
    var sacksAllowed: Int = 0,
    @Basic
    @Column(name = "sacks_forced")
    var sacksForced: Int = 0,
    @Basic
    @Column(name = "interceptions_forced")
    var interceptionsForced: Int = 0,
    @Basic
    @Column(name = "fumbles_forced")
    var fumblesForced: Int = 0,
    @Basic
    @Column(name = "fumbles_recovered")
    var fumblesRecovered: Int = 0,
    @Basic
    @Column(name = "defensive_touchdowns")
    var defensiveTouchdowns: Int = 0,
    // Special Teams Stats (Playbook Totals)
    @Basic
    @Column(name = "field_goals_attempted")
    var fieldGoalsAttempted: Int = 0,
    @Basic
    @Column(name = "field_goals_made")
    var fieldGoalsMade: Int = 0,
    @Basic
    @Column(name = "field_goal_percentage")
    var fieldGoalPercentage: Double = 0.0,
    @Basic
    @Column(name = "longest_field_goal")
    var longestFieldGoal: Int = 0,
    @Basic
    @Column(name = "punts")
    var punts: Int = 0,
    @Basic
    @Column(name = "longest_punt")
    var longestPunt: Int = 0,
    @Basic
    @Column(name = "kickoff_return_touchdowns")
    var kickoffReturnTouchdowns: Int = 0,
    @Basic
    @Column(name = "punt_return_touchdowns")
    var puntReturnTouchdowns: Int = 0,
    // Performance Metrics (Playbook Averages)
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
    @Column(name = "average_diff")
    var averageDiff: Double = 0.0,
    @Basic
    @Column(name = "average_response_speed")
    var averageResponseSpeed: Double = 0.0,
    // Metadata
    @Basic
    @Column(name = "last_modified_ts")
    var lastModifiedTs: String? = null,
) {
    constructor() : this(
        offensivePlaybook = OffensivePlaybook.PRO,
        defensivePlaybook = DefensivePlaybook.THREE_FOUR,
        seasonNumber = 0,
    )
}
