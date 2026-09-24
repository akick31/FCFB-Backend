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
@Table(name = "team_uniform_history")
class TeamUniformHistory {
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
    @Column(name = "primary_color")
    var primaryColor: String? = null

    @Basic
    @Column(name = "secondary_color")
    var secondaryColor: String? = null

    @Basic
    @Column(name = "tertiary_color")
    var tertiaryColor: String? = null

    @Basic
    @Column(name = "helmet_color")
    var helmetColor: String? = null

    @Basic
    @Column(name = "facemask_color")
    var facemaskColor: String? = null

    @Basic
    @Column(name = "helmet_logo_mode")
    var helmetLogoMode: String = TeamUniformCurrent.DEFAULT_HELMET_LOGO_MODE

    @Basic
    @Column(name = "jersey_color")
    var jerseyColor: String? = null

    @Basic
    @Column(name = "number_color")
    var numberColor: String? = null

    @Basic
    @Column(name = "number_outline_color")
    var numberOutlineColor: String? = null

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

    @Basic
    @Column(name = "stripe_color")
    var stripeColor: String? = null

    @Suppress("LongParameterList")
    constructor(
        team: String,
        seasonNumber: Int,
        week: Int,
        primaryColor: String?,
        secondaryColor: String?,
        tertiaryColor: String?,
        helmetColor: String?,
        facemaskColor: String?,
        helmetLogoMode: String,
        jerseyColor: String?,
        numberColor: String?,
        numberOutlineColor: String?,
        pantsColor: String?,
        logoUrl: String?,
        hasLogo: Boolean,
        hasStripe: Boolean,
        stripeColor: String?,
    ) {
        this.team = team
        this.seasonNumber = seasonNumber
        this.week = week
        this.primaryColor = primaryColor
        this.secondaryColor = secondaryColor
        this.tertiaryColor = tertiaryColor
        this.helmetColor = helmetColor
        this.facemaskColor = facemaskColor
        this.helmetLogoMode = helmetLogoMode
        this.jerseyColor = jerseyColor
        this.numberColor = numberColor
        this.numberOutlineColor = numberOutlineColor
        this.pantsColor = pantsColor
        this.logoUrl = logoUrl
        this.hasLogo = hasLogo
        this.hasStripe = hasStripe
        this.stripeColor = stripeColor
    }

    constructor()
}
