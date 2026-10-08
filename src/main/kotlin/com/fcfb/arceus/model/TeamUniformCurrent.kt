package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/** A team's live uniform configuration, snapshotted into [TeamUniformHistory] when a game starts. */
@Entity
@Table(name = "team_uniform_current")
class TeamUniformCurrent {
    @Id
    @Column(name = "team")
    lateinit var team: String

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

    /** The alternate shell, worn when the opponent's helmet is too close to this team's. Must differ from [helmetColor]. */
    @Basic
    @Column(name = "secondary_helmet_color")
    var secondaryHelmetColor: String? = null

    @Basic
    @Column(name = "helmet_number_color")
    var helmetNumberColor: String = DEFAULT_HELMET_NUMBER_COLOR

    @Basic
    @Column(name = "helmet_number_font")
    var helmetNumberFont: String? = null

    @Basic
    @Column(name = "jersey_number_font")
    var jerseyNumberFont: String? = null

    @Basic
    @Column(name = "facemask_color")
    var facemaskColor: String = DEFAULT_FACEMASK_COLOR

    @Basic
    @Column(name = "helmet_logo_mode")
    var helmetLogoMode: String = DEFAULT_HELMET_LOGO_MODE

    @Basic
    @Column(name = "helmet_logo_source")
    var helmetLogoSource: String = DEFAULT_LOGO_SOURCE

    @Basic
    @Column(name = "logo_url")
    var logoUrl: String? = null

    @Basic
    @Column(name = "logo_size")
    var logoSize: Double = DEFAULT_LOGO_SIZE

    @Basic
    @Column(name = "logo_x")
    var logoX: Double = 0.0

    @Basic
    @Column(name = "logo_y")
    var logoY: Double = 0.0

    @Basic
    @Column(name = "logo_rotation")
    var logoRotation: Double = 0.0

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
    var stripeType: String = DEFAULT_STRIPE_TYPE

    @Basic
    @Column(name = "secondary_stripe_color")
    var secondaryStripeColor: String? = null

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
    @Column(name = "number_top_text")
    var numberTopText: String? = null

    companion object {
        const val DEFAULT_FACEMASK_COLOR = "#FFFFFF"
        const val DEFAULT_HELMET_NUMBER_COLOR = "#FFFFFF"
        const val DEFAULT_HELMET_LOGO_MODE = "MAIN"
        const val DEFAULT_LOGO_SIZE = 1.0
        const val DEFAULT_LOGO_SOURCE = "PRIMARY"
        const val DEFAULT_STRIPE_TYPE = "SINGLE"
    }
}
