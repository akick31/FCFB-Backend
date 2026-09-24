package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/** One bowl's field appearance. Left and right are the two ends of the field, each belonging to one team. */
@Entity
@Table(name = "bowl_field")
class BowlField {
    @Id
    @Column(name = "bowl")
    lateinit var bowl: String

    @Basic
    @Column(name = "turf_color")
    var turfColor: String = TeamField.DEFAULT_TURF_COLOR

    @Basic
    @Column(name = "end_zone_fill")
    var endZoneFill: String = DEFAULT_END_ZONE_FILL

    @Basic
    @Column(name = "end_zone_font")
    var endZoneFont: String = TeamField.DEFAULT_END_ZONE_FONT

    /** A custom wordmark replaces the team name in that end zone. */
    @Basic
    @Column(name = "left_end_zone_logo_url")
    var leftEndZoneLogoUrl: String? = null

    @Basic
    @Column(name = "right_end_zone_logo_url")
    var rightEndZoneLogoUrl: String? = null

    @Basic
    @Column(name = "show_conference_logos", columnDefinition = "tinyint(1)")
    var showConferenceLogos: Boolean = true

    @Basic
    @Column(name = "yard_number_source")
    var yardNumberSource: String = DEFAULT_YARD_NUMBER_SOURCE

    /** Only consulted when the source is FIXED. */
    @Basic
    @Column(name = "yard_number_outline_color")
    var yardNumberOutlineColor: String? = null

    @Basic
    @Column(name = "left_oob_line_color")
    var leftOobLineColor: String? = null

    @Basic
    @Column(name = "right_oob_line_color")
    var rightOobLineColor: String? = null

    @Basic
    @Column(name = "red_zone_enabled", columnDefinition = "tinyint(1)")
    var redZoneEnabled: Boolean = true

    /** Null keeps the red zone marker team colored per side. */
    @Basic
    @Column(name = "red_zone_border_color")
    var redZoneBorderColor: String? = null

    @Basic
    @Column(name = "wall_color")
    var wallColor: String? = null

    @Basic
    @Column(name = "wall_design")
    var wallDesign: String = DEFAULT_WALL_DESIGN

    @Basic
    @Column(name = "wall_text")
    var wallText: String? = null

    @Basic
    @Column(name = "wall_text_outline_color")
    var wallTextOutlineColor: String? = null

    companion object {
        const val DEFAULT_END_ZONE_FILL = "PRIMARY"
        const val DEFAULT_YARD_NUMBER_SOURCE = "TEAM_PER_SIDE"
        const val DEFAULT_WALL_DESIGN = "TEXT_WITH_LOGOS"
    }
}
