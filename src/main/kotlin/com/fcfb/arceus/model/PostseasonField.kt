package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.MappedSuperclass

/** Field appearance shared by the postseason games that are not bowls. Null colors keep the built-in look. */
@MappedSuperclass
abstract class PostseasonField {
    @Basic
    @Column(name = "turf_color")
    var turfColor: String = TeamField.DEFAULT_TURF_COLOR

    @Basic
    @Column(name = "end_zone_font")
    var endZoneFont: String = TeamField.DEFAULT_END_ZONE_FONT

    @Basic
    @Column(name = "center_logo_url")
    var centerLogoUrl: String? = null

    @Basic
    @Column(name = "wall_color")
    var wallColor: String? = null

    @Basic
    @Column(name = "wall_design")
    var wallDesign: String = BowlField.DEFAULT_WALL_DESIGN

    @Basic
    @Column(name = "wall_text")
    var wallText: String? = null

    @Basic
    @Column(name = "wall_text_outline_color")
    var wallTextOutlineColor: String? = null

    @Basic
    @Column(name = "goal_post_color")
    var goalPostColor: String = TeamField.DEFAULT_GOAL_POST_COLOR

    @Basic
    @Column(name = "goal_post_style")
    var goalPostStyle: String = TeamField.DEFAULT_GOAL_POST_STYLE

    @Basic
    @Column(name = "yard_number_outline_color")
    var yardNumberOutlineColor: String? = null

    @Basic
    @Column(name = "red_zone_border_color")
    var redZoneBorderColor: String? = null

    @Basic
    @Column(name = "sideline_accent_color")
    var sidelineAccentColor: String? = null
}
