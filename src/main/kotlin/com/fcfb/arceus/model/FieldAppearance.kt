package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.MappedSuperclass

/** The field appearance columns shared by a team's home field and a single game's frozen field. */
@MappedSuperclass
abstract class FieldAppearance {
    @Basic
    @Column(name = "turf_color")
    var turfColor: String = DEFAULT_TURF_COLOR

    @Basic
    @Column(name = "end_zone_color")
    var endZoneColor: String? = null

    @Basic
    @Column(name = "end_zone_text_color")
    var endZoneTextColor: String? = null

    @Basic
    @Column(name = "end_zone_outline_color")
    var endZoneOutlineColor: String? = null

    @Basic
    @Column(name = "end_zone_text_left")
    var endZoneTextLeft: String? = null

    @Basic
    @Column(name = "end_zone_text_right")
    var endZoneTextRight: String? = null

    @Basic
    @Column(name = "end_zone_logo_enabled", columnDefinition = "tinyint(1)")
    var endZoneLogoEnabled: Boolean = false

    @Basic
    @Column(name = "end_zone_logo_source")
    var endZoneLogoSource: String = "PRIMARY"

    @Basic
    @Column(name = "end_zone_logo_url")
    var endZoneLogoUrl: String? = null

    @Basic
    @Column(name = "end_zone_logo_size")
    var endZoneLogoSize: Double = 0.85

    @Basic
    @Column(name = "end_zone_outline_enabled", columnDefinition = "tinyint(1)")
    var endZoneOutlineEnabled: Boolean = true

    @Basic
    @Column(name = "wall_logo_source")
    var wallLogoSource: String = "NONE"

    @Basic
    @Column(name = "wall_logo_url")
    var wallLogoUrl: String? = null

    /** Right-wall overrides; when null the right end falls back to the shared (left) wall value. */
    @Basic
    @Column(name = "right_wall_logo_source")
    var rightWallLogoSource: String? = null

    @Basic
    @Column(name = "right_wall_logo_url")
    var rightWallLogoUrl: String? = null

    @Basic
    @Column(name = "right_wall_color")
    var rightWallColor: String? = null

    @Basic
    @Column(name = "right_wall_text_outline_color")
    var rightWallTextOutlineColor: String? = null

    @Basic
    @Column(name = "end_zone_font")
    var endZoneFont: String = DEFAULT_END_ZONE_FONT

    @Basic
    @Column(name = "midfield_logo_url")
    var midfieldLogoUrl: String? = null

    @Basic
    @Column(name = "midfield_logo_source")
    var midfieldLogoSource: String = "CUSTOM"

    @Basic
    @Column(name = "field_number_outline_color")
    var fieldNumberOutlineColor: String? = null

    @Basic
    @Column(name = "red_zone_border_color")
    var redZoneBorderColor: String? = null

    @Basic
    @Column(name = "midfield_border_color")
    var midfieldBorderColor: String? = null

    @Basic
    @Column(name = "oob_line_color")
    var oobLineColor: String? = null

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
    var goalPostColor: String = DEFAULT_GOAL_POST_COLOR

    @Basic
    @Column(name = "goal_post_style")
    var goalPostStyle: String = DEFAULT_GOAL_POST_STYLE

    @Basic
    @Column(name = "quarter_logo_url")
    var quarterLogoUrl: String? = null

    @Basic
    @Column(name = "quarter_logo_source")
    var quarterLogoSource: String = "CUSTOM"

    @Basic
    @Column(name = "midfield_logo_size")
    var midfieldLogoSize: Double = 1.0

    @Basic
    @Column(name = "conference_logo_size")
    var conferenceLogoSize: Double = 1.0

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
    @Column(name = "conference_logo_color_map", columnDefinition = "text")
    var conferenceLogoColorMap: String? = null

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

    fun copyAppearanceInto(target: FieldAppearance) {
        target.turfColor = turfColor
        target.endZoneColor = endZoneColor
        target.endZoneTextColor = endZoneTextColor
        target.endZoneOutlineColor = endZoneOutlineColor
        target.endZoneTextLeft = endZoneTextLeft
        target.endZoneTextRight = endZoneTextRight
        target.endZoneLogoEnabled = endZoneLogoEnabled
        target.endZoneLogoSource = endZoneLogoSource
        target.endZoneLogoUrl = endZoneLogoUrl
        target.endZoneLogoSize = endZoneLogoSize
        target.endZoneOutlineEnabled = endZoneOutlineEnabled
        target.wallLogoSource = wallLogoSource
        target.wallLogoUrl = wallLogoUrl
        target.rightWallLogoSource = rightWallLogoSource
        target.rightWallLogoUrl = rightWallLogoUrl
        target.rightWallColor = rightWallColor
        target.rightWallTextOutlineColor = rightWallTextOutlineColor
        target.endZoneFont = endZoneFont
        target.midfieldLogoUrl = midfieldLogoUrl
        target.midfieldLogoSource = midfieldLogoSource
        target.fieldNumberOutlineColor = fieldNumberOutlineColor
        target.redZoneBorderColor = redZoneBorderColor
        target.midfieldBorderColor = midfieldBorderColor
        target.oobLineColor = oobLineColor
        target.wallColor = wallColor
        target.wallDesign = wallDesign
        target.wallText = wallText
        target.wallTextOutlineColor = wallTextOutlineColor
        target.wallTextFont = wallTextFont
        target.goalPostColor = goalPostColor
        target.goalPostStyle = goalPostStyle
        target.quarterLogoUrl = quarterLogoUrl
        target.quarterLogoSource = quarterLogoSource
        target.midfieldLogoSize = midfieldLogoSize
        target.conferenceLogoSize = conferenceLogoSize
        target.leftEndZoneFont = leftEndZoneFont
        target.rightEndZoneFont = rightEndZoneFont
        target.rightWallDesign = rightWallDesign
        target.rightWallText = rightWallText
        target.yardNumberFont = yardNumberFont
        target.conferenceLogoColorMap = conferenceLogoColorMap
        target.yardNumberArrowAlign = yardNumberArrowAlign
        target.yardNumberOutlineWidth = yardNumberOutlineWidth
        target.endZoneOutlineWidth = endZoneOutlineWidth
        target.wallTextOutlineWidth = wallTextOutlineWidth
    }

    companion object {
        const val DEFAULT_TURF_COLOR = "#226633"
        const val DEFAULT_END_ZONE_FONT = "CLASSIC"
        const val DEFAULT_WALL_DESIGN = "REPEATING_LOGOS"
        const val DEFAULT_GOAL_POST_COLOR = "#FFCD00"
        const val DEFAULT_GOAL_POST_STYLE = "Y"
    }
}
