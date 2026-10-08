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
    @Column(name = "secondary_helmet_color")
    var secondaryHelmetColor: String? = null

    @Basic
    @Column(name = "helmet_number_color")
    var helmetNumberColor: String? = null

    @Basic
    @Column(name = "helmet_number_font")
    var helmetNumberFont: String? = null

    @Basic
    @Column(name = "facemask_color")
    var facemaskColor: String? = null

    @Basic
    @Column(name = "helmet_logo_mode")
    var helmetLogoMode: String = TeamUniformCurrent.DEFAULT_HELMET_LOGO_MODE

    @Basic
    @Column(name = "helmet_logo_source")
    var helmetLogoSource: String = TeamUniformCurrent.DEFAULT_LOGO_SOURCE

    @Basic
    @Column(name = "jersey_color")
    var jerseyColor: String? = null

    @Basic
    @Column(name = "number_color")
    var numberColor: String? = null

    @Basic
    @Column(name = "jersey_number_font")
    var jerseyNumberFont: String? = null

    @Basic
    @Column(name = "number_outline_color")
    var numberOutlineColor: String? = null

    @Basic
    @Column(name = "away_number_color")
    var awayNumberColor: String? = null

    @Basic
    @Column(name = "away_number_outline_color")
    var awayNumberOutlineColor: String? = null

    @Basic
    @Column(name = "alt_facemask_color")
    var altFacemaskColor: String? = null

    @Basic
    @Column(name = "alt_helmet_number_color")
    var altHelmetNumberColor: String? = null

    @Basic
    @Column(name = "alt_helmet_logo_mode")
    var altHelmetLogoMode: String? = null

    @Basic
    @Column(name = "alt_helmet_logo_source")
    var altHelmetLogoSource: String? = null

    @Basic
    @Column(name = "alt_has_logo", columnDefinition = "tinyint(1)")
    var altHasLogo: Boolean? = null

    @Basic
    @Column(name = "alt_logo_url")
    var altLogoUrl: String? = null

    @Basic
    @Column(name = "alt_logo_size")
    var altLogoSize: Double? = null

    @Basic
    @Column(name = "alt_logo_x")
    var altLogoX: Double? = null

    @Basic
    @Column(name = "alt_logo_y")
    var altLogoY: Double? = null

    @Basic
    @Column(name = "alt_logo_rotation")
    var altLogoRotation: Double? = null

    @Basic
    @Column(name = "alt_has_stripe", columnDefinition = "tinyint(1)")
    var altHasStripe: Boolean? = null

    @Basic
    @Column(name = "alt_stripe_color")
    var altStripeColor: String? = null

    @Basic
    @Column(name = "alt_stripe_type")
    var altStripeType: String? = null

    @Basic
    @Column(name = "alt_secondary_stripe_color")
    var altSecondaryStripeColor: String? = null

    @Basic
    @Column(name = "pants_color")
    var pantsColor: String? = null

    @Basic
    @Column(name = "away_pants_color")
    var awayPantsColor: String? = null

    @Basic
    @Column(name = "has_shoulder_stripe", columnDefinition = "tinyint(1)")
    var hasShoulderStripe: Boolean? = null

    @Basic
    @Column(name = "shoulder_stripe_color")
    var shoulderStripeColor: String? = null

    @Basic
    @Column(name = "jersey_text")
    var jerseyText: String? = null

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

    @Basic
    @Column(name = "stripe_type")
    var stripeType: String = TeamUniformCurrent.DEFAULT_STRIPE_TYPE

    @Basic
    @Column(name = "secondary_stripe_color")
    var secondaryStripeColor: String? = null

    @Basic
    @Column(name = "logo_size")
    var logoSize: Double = TeamUniformCurrent.DEFAULT_LOGO_SIZE

    @Basic
    @Column(name = "logo_x")
    var logoX: Double = 0.0

    @Basic
    @Column(name = "logo_y")
    var logoY: Double = 0.0

    @Basic
    @Column(name = "logo_rotation")
    var logoRotation: Double = 0.0

    @Suppress("LongParameterList")
    constructor(
        team: String,
        seasonNumber: Int,
        week: Int,
        primaryColor: String?,
        secondaryColor: String?,
        tertiaryColor: String?,
        helmetColor: String?,
        secondaryHelmetColor: String?,
        helmetNumberColor: String?,
        facemaskColor: String?,
        helmetLogoMode: String,
        helmetLogoSource: String,
        jerseyColor: String?,
        numberColor: String?,
        numberOutlineColor: String?,
        awayNumberColor: String?,
        awayNumberOutlineColor: String?,
        pantsColor: String?,
        logoUrl: String?,
        hasLogo: Boolean,
        hasStripe: Boolean,
        stripeColor: String?,
        stripeType: String,
        secondaryStripeColor: String?,
        logoSize: Double,
        logoX: Double,
        logoY: Double,
        logoRotation: Double,
        helmetNumberFont: String? = null,
        jerseyNumberFont: String? = null,
        awayPantsColor: String? = null,
        hasShoulderStripe: Boolean? = null,
        shoulderStripeColor: String? = null,
        jerseyText: String? = null,
    ) {
        this.team = team
        this.seasonNumber = seasonNumber
        this.week = week
        this.primaryColor = primaryColor
        this.secondaryColor = secondaryColor
        this.tertiaryColor = tertiaryColor
        this.helmetColor = helmetColor
        this.secondaryHelmetColor = secondaryHelmetColor
        this.helmetNumberColor = helmetNumberColor
        this.facemaskColor = facemaskColor
        this.helmetLogoMode = helmetLogoMode
        this.helmetLogoSource = helmetLogoSource
        this.jerseyColor = jerseyColor
        this.numberColor = numberColor
        this.numberOutlineColor = numberOutlineColor
        this.awayNumberColor = awayNumberColor
        this.awayNumberOutlineColor = awayNumberOutlineColor
        this.pantsColor = pantsColor
        this.logoUrl = logoUrl
        this.hasLogo = hasLogo
        this.hasStripe = hasStripe
        this.stripeColor = stripeColor
        this.stripeType = stripeType
        this.secondaryStripeColor = secondaryStripeColor
        this.logoSize = logoSize
        this.logoX = logoX
        this.logoY = logoY
        this.logoRotation = logoRotation
        this.helmetNumberFont = helmetNumberFont
        this.jerseyNumberFont = jerseyNumberFont
        this.awayPantsColor = awayPantsColor
        this.hasShoulderStripe = hasShoulderStripe
        this.shoulderStripeColor = shoulderStripeColor
        this.jerseyText = jerseyText
    }

    constructor()
}
