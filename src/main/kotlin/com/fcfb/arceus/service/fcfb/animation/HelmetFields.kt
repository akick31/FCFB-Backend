package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.TeamUniformHistory

/**
 * Resolves helmet fields for either the primary or the secondary ("alternate") helmet. Any unset
 * secondary value falls back to the matching primary value, so a team that never customizes its
 * alternate still renders the same helmet in a different shell color.
 */
object HelmetFields {
    fun facemaskColor(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): String? = if (alt) snapshot?.altFacemaskColor ?: snapshot?.facemaskColor else snapshot?.facemaskColor

    fun helmetNumberColor(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): String? = if (alt) snapshot?.altHelmetNumberColor ?: snapshot?.helmetNumberColor else snapshot?.helmetNumberColor

    fun helmetLogoMode(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): String? = if (alt) snapshot?.altHelmetLogoMode ?: snapshot?.helmetLogoMode else snapshot?.helmetLogoMode

    fun helmetLogoSource(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): String? = if (alt) snapshot?.altHelmetLogoSource ?: snapshot?.helmetLogoSource else snapshot?.helmetLogoSource

    fun hasLogo(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Boolean = if (alt) snapshot?.altHasLogo ?: snapshot?.hasLogo ?: true else snapshot?.hasLogo ?: true

    fun logoUrl(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): String? = if (alt) snapshot?.altLogoUrl ?: snapshot?.logoUrl else snapshot?.logoUrl

    fun logoSize(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Double = if (alt) snapshot?.altLogoSize ?: snapshot?.logoSize ?: 1.0 else snapshot?.logoSize ?: 1.0

    fun logoX(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Double = if (alt) snapshot?.altLogoX ?: snapshot?.logoX ?: 0.0 else snapshot?.logoX ?: 0.0

    fun logoY(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Double = if (alt) snapshot?.altLogoY ?: snapshot?.logoY ?: 0.0 else snapshot?.logoY ?: 0.0

    fun logoRotation(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Double = if (alt) snapshot?.altLogoRotation ?: snapshot?.logoRotation ?: 0.0 else snapshot?.logoRotation ?: 0.0

    fun hasStripe(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): Boolean = if (alt) snapshot?.altHasStripe ?: snapshot?.hasStripe ?: false else snapshot?.hasStripe ?: false

    fun stripeColor(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): String? = if (alt) snapshot?.altStripeColor ?: snapshot?.stripeColor else snapshot?.stripeColor

    fun stripeType(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): String? = if (alt) snapshot?.altStripeType ?: snapshot?.stripeType else snapshot?.stripeType

    fun secondaryStripeColor(
        snapshot: TeamUniformHistory?,
        alt: Boolean,
    ): String? = if (alt) snapshot?.altSecondaryStripeColor ?: snapshot?.secondaryStripeColor else snapshot?.secondaryStripeColor
}
