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
    @Column(name = "left_end_zone_text")
    var leftEndZoneText: String? = null

    @Basic
    @Column(name = "right_end_zone_text")
    var rightEndZoneText: String? = null

    @Basic
    @Column(name = "left_end_zone_logo_source")
    var leftEndZoneLogoSource: String = "CUSTOM"

    @Basic
    @Column(name = "right_end_zone_logo_source")
    var rightEndZoneLogoSource: String = "CUSTOM"

    @Basic
    @Column(name = "left_end_zone_font")
    var leftEndZoneFont: String? = null

    @Basic
    @Column(name = "right_end_zone_font")
    var rightEndZoneFont: String? = null

    @Basic
    @Column(name = "right_wall_design")
    var rightWallDesign: String? = null

    @Basic
    @Column(name = "right_wall_text")
    var rightWallText: String? = null

    @Basic
    @Column(name = "yard_number_font")
    var yardNumberFont: String? = null

    @Basic
    @Column(name = "yard_number_arrow_align")
    var yardNumberArrowAlign: String? = null

    @Basic
    @Column(name = "yard_number_outline_width")
    var yardNumberOutlineWidth: Double = 1.0

    @Basic
    @Column(name = "end_zone_outline_width")
    var endZoneOutlineWidth: Double = 1.0

    @Basic
    @Column(name = "wall_text_outline_width")
    var wallTextOutlineWidth: Double = 1.0

    @Basic
    @Column(name = "conference_logo_size")
    var conferenceLogoSize: Double = 1.0

    @Basic
    @Column(name = "show_conference_logos", columnDefinition = "tinyint(1)")
    var showConferenceLogos: Boolean = true

    @Basic
    @Column(name = "yard_number_source")
    var yardNumberSource: String = DEFAULT_YARD_NUMBER_SOURCE

    /** Which team color slot each defending team contributes when the source is the defending team. */
    @Basic
    @Column(name = "yard_number_team_slot")
    var yardNumberTeamSlot: String? = null

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
    var redZoneEnabled: Boolean = false

    /** Null keeps the red zone marker team colored per side. */
    @Basic
    @Column(name = "red_zone_border_color")
    var redZoneBorderColor: String? = null

    @Basic
    @Column(name = "midfield_border_color")
    var midfieldBorderColor: String? = null

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

    @Basic
    @Column(name = "wall_text_font")
    var wallTextFont: String? = null

    @Basic
    @Column(name = "goal_post_color")
    var goalPostColor: String = TeamField.DEFAULT_GOAL_POST_COLOR

    @Basic
    @Column(name = "goal_post_style")
    var goalPostStyle: String = TeamField.DEFAULT_GOAL_POST_STYLE

    fun copyInto(target: BowlField) {
        target.turfColor = turfColor
        target.endZoneFill = endZoneFill
        target.endZoneFont = endZoneFont
        target.leftEndZoneLogoUrl = leftEndZoneLogoUrl
        target.rightEndZoneLogoUrl = rightEndZoneLogoUrl
        target.leftEndZoneText = leftEndZoneText
        target.rightEndZoneText = rightEndZoneText
        target.leftEndZoneLogoSource = leftEndZoneLogoSource
        target.rightEndZoneLogoSource = rightEndZoneLogoSource
        target.leftEndZoneFont = leftEndZoneFont
        target.rightEndZoneFont = rightEndZoneFont
        target.rightWallDesign = rightWallDesign
        target.rightWallText = rightWallText
        target.yardNumberFont = yardNumberFont
        target.yardNumberArrowAlign = yardNumberArrowAlign
        target.yardNumberOutlineWidth = yardNumberOutlineWidth
        target.endZoneOutlineWidth = endZoneOutlineWidth
        target.wallTextOutlineWidth = wallTextOutlineWidth
        target.conferenceLogoSize = conferenceLogoSize
        target.showConferenceLogos = showConferenceLogos
        target.yardNumberSource = yardNumberSource
        target.yardNumberTeamSlot = yardNumberTeamSlot
        target.yardNumberOutlineColor = yardNumberOutlineColor
        target.leftOobLineColor = leftOobLineColor
        target.rightOobLineColor = rightOobLineColor
        target.redZoneEnabled = redZoneEnabled
        target.redZoneBorderColor = redZoneBorderColor
        target.midfieldBorderColor = midfieldBorderColor
        target.wallColor = wallColor
        target.wallDesign = wallDesign
        target.wallText = wallText
        target.wallTextOutlineColor = wallTextOutlineColor
        target.wallTextFont = wallTextFont
        target.goalPostColor = goalPostColor
        target.goalPostStyle = goalPostStyle
    }

    companion object {
        const val DEFAULT_END_ZONE_FILL = "PRIMARY"
        const val DEFAULT_YARD_NUMBER_SOURCE = "TEAM_PER_SIDE"
        const val DEFAULT_WALL_DESIGN = "TEXT_WITH_LOGOS"
    }
}
