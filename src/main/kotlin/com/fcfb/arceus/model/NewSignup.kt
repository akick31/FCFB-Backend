package com.fcfb.arceus.model

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.user.CoachPosition
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
@Table(name = "new_signup")
class NewSignup {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id", columnDefinition = "int(11)")
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

    @Basic
    @Column(name = "salt")
    lateinit var salt: String

    @Basic
    @Column(name = "team_choice_one")
    lateinit var teamChoiceOne: String

    @Basic
    @Column(name = "team_choice_two")
    lateinit var teamChoiceTwo: String

    @Basic
    @Column(name = "team_choice_three")
    lateinit var teamChoiceThree: String

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "offensive_playbook")
    lateinit var offensivePlaybook: OffensivePlaybook

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "defensive_playbook")
    lateinit var defensivePlaybook: DefensivePlaybook

    @Basic
    @Column(name = "approved")
    var approved: Boolean = false

    @Basic
    @Column(name = "verification_token")
    lateinit var verificationToken: String

    constructor(
        username: String,
        coachName: String,
        discordTag: String,
        discordId: String?,
        email: String,
        hashedEmail: String,
        password: String,
        position: CoachPosition,
        salt: String,
        teamChoiceOne: String,
        teamChoiceTwo: String,
        teamChoiceThree: String,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        approved: Boolean,
        verificationToken: String,
    ) {
        this.username = username
        this.coachName = coachName
        this.discordTag = discordTag
        this.discordId = discordId
        this.email = email
        this.hashedEmail = hashedEmail
        this.password = password
        this.position = position
        this.salt = salt
        this.teamChoiceOne = teamChoiceOne
        this.teamChoiceTwo = teamChoiceTwo
        this.teamChoiceThree = teamChoiceThree
        this.offensivePlaybook = offensivePlaybook
        this.defensivePlaybook = defensivePlaybook
        this.approved = approved
        this.verificationToken = verificationToken
    }

    constructor()
}
