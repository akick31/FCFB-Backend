package com.fcfb.arceus.model

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
import com.fcfb.arceus.enums.user.UserRole
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
@Table(name = "user")
class User {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    var id: Long = 0

    @Basic
    @Column(name = "username")
    lateinit var username: String

    @Basic
    @Column(name = "coach_name")
    lateinit var coachName: String

    @Basic
    @Column(name = "discord_tag")
    lateinit var discordTag: String

    @Basic
    @Column(name = "discord_id")
    var discordId: String? = null

    @Basic
    @Column(name = "email")
    lateinit var email: String

    @Basic
    @Column(name = "hashed_email")
    lateinit var hashedEmail: String

    @Basic
    @Column(name = "password")
    lateinit var password: String

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "position")
    lateinit var position: CoachPosition

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "role")
    var role: UserRole = UserRole.USER

    @Basic
    @Column(name = "salt")
    lateinit var salt: String

    @Basic
    @Column(name = "team")
    var team: String? = null

    @Basic
    @Column(name = "delay_of_game_instances")
    var delayOfGameInstances: Int = 0

    @Basic
    @Column(name = "wins")
    var wins: Int = 0

    @Basic
    @Column(name = "losses")
    var losses: Int = 0

    @Basic
    @Column(name = "conference_wins")
    var conferenceWins: Int = 0

    @Basic
    @Column(name = "conference_losses")
    var conferenceLosses: Int = 0

    @Basic
    @Column(name = "conference_championship_wins")
    var conferenceChampionshipWins: Int = 0

    @Basic
    @Column(name = "conference_championship_losses")
    var conferenceChampionshipLosses: Int = 0

    @Basic
    @Column(name = "bowl_wins")
    var bowlWins: Int = 0

    @Basic
    @Column(name = "bowl_losses")
    var bowlLosses: Int = 0

    @Basic
    @Column(name = "playoff_wins")
    var playoffWins: Int = 0

    @Basic
    @Column(name = "playoff_losses")
    var playoffLosses: Int = 0

    @Basic
    @Column(name = "national_championship_wins")
    var nationalChampionshipWins: Int = 0

    @Basic
    @Column(name = "national_championship_losses")
    var nationalChampionshipLosses: Int = 0

    @Basic
    @Column(name = "win_percentage")
    var winPercentage: Double = 0.0

    @Basic
    @Column(name = "offensive_playbook")
    lateinit var offensivePlaybook: OffensivePlaybook

    @Basic
    @Column(name = "defensive_playbook")
    lateinit var defensivePlaybook: DefensivePlaybook

    @Basic
    @Column(name = "delay_of_game_warning_opt_out", columnDefinition = "tinyint(1)")
    var delayOfGameWarningOptOut: Boolean = false

    @Basic
    @Column(name = "average_response_time")
    var averageResponseTime: Double = 0.0

    @Basic
    @Column(name = "reset_token")
    var resetToken: String? = null

    @Basic
    @Column(name = "reset_token_expiration")
    var resetTokenExpiration: String? = null

    constructor(
        username: String,
        coachName: String,
        discordTag: String,
        discordId: String?,
        email: String,
        hashedEmail: String,
        password: String,
        position: CoachPosition,
        role: UserRole,
        salt: String,
        team: String?,
        delayOfGameInstances: Int,
        wins: Int,
        losses: Int,
        winPercentage: Double,
        conferenceWins: Int,
        conferenceLosses: Int,
        conferenceChampionshipWins: Int,
        conferenceChampionshipLosses: Int,
        bowlWins: Int,
        bowlLosses: Int,
        playoffWins: Int,
        playoffLosses: Int,
        nationalChampionshipWins: Int,
        nationalChampionshipLosses: Int,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        averageResponseTime: Double,
        delayOfGameWarningOptOut: Boolean,
        resetToken: String?,
        resetTokenExpiration: String?,
    ) {
        this.username = username
        this.coachName = coachName
        this.discordTag = discordTag
        this.discordId = discordId
        this.email = email
        this.hashedEmail = hashedEmail
        this.password = password
        this.position = position
        this.role = role
        this.salt = salt
        this.team = team
        this.delayOfGameInstances = delayOfGameInstances
        this.wins = wins
        this.losses = losses
        this.winPercentage = winPercentage
        this.conferenceWins = conferenceWins
        this.conferenceLosses = conferenceLosses
        this.conferenceChampionshipWins = conferenceChampionshipWins
        this.conferenceChampionshipLosses = conferenceChampionshipLosses
        this.bowlWins = bowlWins
        this.bowlLosses = bowlLosses
        this.playoffWins = playoffWins
        this.playoffLosses = playoffLosses
        this.nationalChampionshipWins = nationalChampionshipWins
        this.nationalChampionshipLosses = nationalChampionshipLosses
        this.offensivePlaybook = offensivePlaybook
        this.defensivePlaybook = defensivePlaybook
        this.averageResponseTime = averageResponseTime
        this.delayOfGameWarningOptOut = delayOfGameWarningOptOut
        this.resetToken = resetToken
        this.resetTokenExpiration = resetTokenExpiration
    }

    constructor()
}
