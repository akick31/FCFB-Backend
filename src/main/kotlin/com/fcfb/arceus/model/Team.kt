package com.fcfb.arceus.model

import com.fcfb.arceus.enums.team.Conference
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import com.fcfb.arceus.util.DefensivePlaybookConverter
import com.fcfb.arceus.util.OffensivePlaybookConverter
import com.fcfb.arceus.util.SubdivisionConverter
import org.hibernate.annotations.Type
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "team")
class Team {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    var id: Int = 0

    @Basic
    @Column(name = "name")
    var name: String? = null

    @Basic
    @Column(name = "abbreviation")
    var abbreviation: String? = null

    @Basic
    @Column(name = "short_name")
    var shortName: String? = null

    @Basic
    @Column(name = "logo")
    var logo: String? = null

    @Basic
    @Column(name = "scorebug_logo")
    var scorebugLogo: String? = null

    @Type(type = "json")
    @Column(name = "coach_usernames")
    var coachUsernames: MutableList<String>? = mutableListOf()

    @Type(type = "json")
    @Column(name = "coach_names")
    var coachNames: MutableList<String>? = mutableListOf()

    @Type(type = "json")
    @Column(name = "coach_discord_tags")
    var coachDiscordTags: MutableList<String>? = mutableListOf()

    @Type(type = "json")
    @Column(name = "coach_discord_ids")
    var coachDiscordIds: MutableList<String>? = mutableListOf()

    @Basic
    @Column(name = "primary_color")
    var primaryColor: String? = null

    @Basic
    @Column(name = "secondary_color")
    var secondaryColor: String? = null

    @Basic
    @Column(name = "coaches_poll_ranking")
    var coachesPollRanking: Int? = null

    @Basic
    @Column(name = "playoff_committee_ranking")
    var playoffCommitteeRanking: Int? = null

    @Convert(converter = SubdivisionConverter::class)
    @Column(name = "subdivision")
    var subdivision: Subdivision? = null

    @Convert(converter = OffensivePlaybookConverter::class)
    @Column(name = "offensive_playbook")
    lateinit var offensivePlaybook: OffensivePlaybook

    @Convert(converter = DefensivePlaybookConverter::class)
    @Column(name = "defensive_playbook")
    lateinit var defensivePlaybook: DefensivePlaybook

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "conference")
    var conference: Conference? = null

    @Basic
    @Column(name = "current_wins")
    var currentWins: Int = 0

    @Basic
    @Column(name = "current_losses")
    var currentLosses: Int = 0

    @Basic
    @Column(name = "overall_wins")
    var overallWins: Int = 0

    @Basic
    @Column(name = "overall_losses")
    var overallLosses: Int = 0

    @Basic
    @Column(name = "current_conference_wins")
    var currentConferenceWins: Int = 0

    @Basic
    @Column(name = "current_conference_losses")
    var currentConferenceLosses: Int = 0

    @Basic
    @Column(name = "overall_conference_wins")
    var overallConferenceWins: Int = 0

    @Basic
    @Column(name = "overall_conference_losses")
    var overallConferenceLosses: Int = 0

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
    @Column(name = "is_taken")
    var isTaken: Boolean = false

    @Basic
    @Column(name = "active")
    var active: Boolean = true

    @Basic
    @Column(name = "current_elo")
    var currentElo: Double = 1500.0

    @Basic
    @Column(name = "overall_elo")
    var overallElo: Double = 1500.0

    constructor(
        logo: String?,
        scorebugLogo: String?,
        coachUsernames: MutableList<String>,
        coachNames: MutableList<String>,
        coachDiscordTags: MutableList<String>,
        coachDiscordIds: MutableList<String>,
        name: String?,
        shortName: String?,
        abbreviation: String?,
        primaryColor: String?,
        secondaryColor: String?,
        coachesPollRanking: Int?,
        playoffCommitteeRanking: Int?,
        subdivision: Subdivision?,
        offensivePlaybook: OffensivePlaybook,
        defensivePlaybook: DefensivePlaybook,
        conference: Conference?,
        currentWins: Int,
        currentLosses: Int,
        overallWins: Int,
        overallLosses: Int,
        currentConferenceWins: Int,
        currentConferenceLosses: Int,
        overallConferenceWins: Int,
        overallConferenceLosses: Int,
        conferenceChampionshipWins: Int,
        conferenceChampionshipLosses: Int,
        bowlWins: Int,
        bowlLosses: Int,
        playoffWins: Int,
        playoffLosses: Int,
        nationalChampionshipWins: Int,
        nationalChampionshipLosses: Int,
        isTaken: Boolean,
        active: Boolean,
        currentElo: Double,
        overallElo: Double,
    ) {
        this.logo = logo
        this.scorebugLogo = scorebugLogo
        this.coachUsernames = coachUsernames
        this.coachNames = coachNames
        this.coachDiscordTags = coachDiscordTags
        this.coachDiscordIds = coachDiscordIds
        this.name = name
        this.shortName = shortName
        this.abbreviation = abbreviation
        this.primaryColor = primaryColor
        this.secondaryColor = secondaryColor
        this.coachesPollRanking = coachesPollRanking
        this.playoffCommitteeRanking = playoffCommitteeRanking
        this.subdivision = subdivision
        this.offensivePlaybook = offensivePlaybook
        this.defensivePlaybook = defensivePlaybook
        this.conference = conference
        this.currentWins = currentWins
        this.currentLosses = currentLosses
        this.overallWins = overallWins
        this.overallLosses = overallLosses
        this.currentConferenceWins = currentConferenceWins
        this.currentConferenceLosses = currentConferenceLosses
        this.overallConferenceWins = overallConferenceWins
        this.overallConferenceLosses = overallConferenceLosses
        this.conferenceChampionshipWins = conferenceChampionshipWins
        this.conferenceChampionshipLosses = conferenceChampionshipLosses
        this.bowlWins = bowlWins
        this.bowlLosses = bowlLosses
        this.playoffWins = playoffWins
        this.playoffLosses = playoffLosses
        this.nationalChampionshipWins = nationalChampionshipWins
        this.nationalChampionshipLosses = nationalChampionshipLosses
        this.isTaken = isTaken
        this.active = active
        this.currentElo = currentElo
        this.overallElo = overallElo
    }

    constructor()
}
