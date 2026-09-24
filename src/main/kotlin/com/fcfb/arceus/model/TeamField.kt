package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/** A team's home field appearance. Replaces the compiled-in turf and end zone overrides. */
@Entity
@Table(name = "team_field")
class TeamField {
    @Id
    @Column(name = "team")
    lateinit var team: String

    @Basic
    @Column(name = "turf_color")
    var turfColor: String = DEFAULT_TURF_COLOR

    @Basic
    @Column(name = "end_zone_color")
    var endZoneColor: String? = null

    @Basic
    @Column(name = "end_zone_font")
    var endZoneFont: String = DEFAULT_END_ZONE_FONT

    @Basic
    @Column(name = "midfield_logo_url")
    var midfieldLogoUrl: String? = null

    @Basic
    @Column(name = "field_number_outline_color")
    var fieldNumberOutlineColor: String? = null

    /** Border drawn either side of the white 20-yard line, marking the red zone. */
    @Basic
    @Column(name = "red_zone_border_color")
    var redZoneBorderColor: String? = null

    /** Runs each sideline out to the 20; null leaves the sidelines plain white. */
    @Basic
    @Column(name = "oob_line_color")
    var oobLineColor: String? = null

    /** The wall behind the end zone in the kick view. White is rejected on save. */
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
    @Column(name = "goal_post_color")
    var goalPostColor: String = DEFAULT_GOAL_POST_COLOR

    @Basic
    @Column(name = "goal_post_style")
    var goalPostStyle: String = DEFAULT_GOAL_POST_STYLE

    @Basic
    @Column(name = "quarter_logo_url")
    var quarterLogoUrl: String? = null

    companion object {
        const val DEFAULT_TURF_COLOR = "#226633"
        const val DEFAULT_END_ZONE_FONT = "CLASSIC"
        const val DEFAULT_WALL_DESIGN = "REPEATING_LOGOS"
        const val DEFAULT_GOAL_POST_COLOR = "#FFCD00"
        const val DEFAULT_GOAL_POST_STYLE = "Y"
    }
}
