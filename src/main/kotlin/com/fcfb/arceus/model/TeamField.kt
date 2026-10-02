package com.fcfb.arceus.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/** A team's home field appearance. Replaces the compiled-in turf and end zone overrides. */
@Entity
@Table(name = "team_field")
class TeamField : FieldAppearance() {
    @Id
    @Column(name = "team")
    lateinit var team: String

    companion object {
        const val DEFAULT_TURF_COLOR = FieldAppearance.DEFAULT_TURF_COLOR
        const val DEFAULT_END_ZONE_FONT = FieldAppearance.DEFAULT_END_ZONE_FONT
        const val DEFAULT_WALL_DESIGN = FieldAppearance.DEFAULT_WALL_DESIGN
        const val DEFAULT_GOAL_POST_COLOR = FieldAppearance.DEFAULT_GOAL_POST_COLOR
        const val DEFAULT_GOAL_POST_STYLE = FieldAppearance.DEFAULT_GOAL_POST_STYLE
    }
}
