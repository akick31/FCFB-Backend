package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

/** What a team wore in one game week, captured when the game starts so a play animation always renders that week's uniform. */
@Entity
@Table(name = "team_uniform")
class TeamUniform {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0

    @Basic
    @Column(name = "team")
    lateinit var team: String

    @Basic
    @Column(name = "season_number")
    var seasonNumber: Int = 0

    @Basic
    @Column(name = "week")
    var week: Int = 0

    @Basic
    @Column(name = "helmet_color")
    var helmetColor: String? = null

    @Basic
    @Column(name = "facemask_color")
    var facemaskColor: String? = null

    @Basic
    @Column(name = "jersey_color")
    var jerseyColor: String? = null

    @Basic
    @Column(name = "pants_color")
    var pantsColor: String? = null

    @Basic
    @Column(name = "logo_url")
    var logoUrl: String? = null

    @Basic
    @Column(name = "has_logo", columnDefinition = "tinyint(1)")
    var hasLogo: Boolean = true

    @Basic
    @Column(name = "has_stripe", columnDefinition = "tinyint(1)")
    var hasStripe: Boolean = false

    constructor(
        team: String,
        seasonNumber: Int,
        week: Int,
        helmetColor: String?,
        facemaskColor: String?,
        jerseyColor: String?,
        pantsColor: String?,
        logoUrl: String?,
        hasLogo: Boolean,
        hasStripe: Boolean,
    ) {
        this.team = team
        this.seasonNumber = seasonNumber
        this.week = week
        this.helmetColor = helmetColor
        this.facemaskColor = facemaskColor
        this.jerseyColor = jerseyColor
        this.pantsColor = pantsColor
        this.logoUrl = logoUrl
        this.hasLogo = hasLogo
        this.hasStripe = hasStripe
    }

    constructor()
}
