package com.fcfb.arceus.model.appearance

import com.fcfb.arceus.enums.animation.PostseasonCategory
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "game_look")
class GameLook {
    @Id
    @Column(name = "game_id")
    var gameId: Int = 0

    @Basic
    @Column(name = "stadium_captured")
    var stadiumCaptured: Boolean = false

    @Embedded
    var stadium: StadiumDesign = StadiumDesign()

    @Basic
    @Column(name = "bowl_field_json", columnDefinition = "LONGTEXT")
    var bowlFieldJson: String? = null

    @Basic
    @Column(name = "postseason_field_json", columnDefinition = "LONGTEXT")
    var postseasonFieldJson: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "postseason_kind")
    var postseasonKind: PostseasonCategory? = null
}
