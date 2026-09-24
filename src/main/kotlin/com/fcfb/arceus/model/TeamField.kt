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

    @Basic
    @Column(name = "quarter_logo_url")
    var quarterLogoUrl: String? = null

    companion object {
        const val DEFAULT_TURF_COLOR = "#226633"
        const val DEFAULT_END_ZONE_FONT = "CLASSIC"
    }
}
