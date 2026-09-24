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

    @Basic
    @Column(name = "facemask_color")
    var facemaskColor: String = DEFAULT_FACEMASK_COLOR

    @Basic
    @Column(name = "helmet_logo_mode")
    var helmetLogoMode: String = DEFAULT_HELMET_LOGO_MODE

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

    companion object {
        const val DEFAULT_FACEMASK_COLOR = "#FFFFFF"
        const val DEFAULT_HELMET_LOGO_MODE = "MAIN"
    }
}
