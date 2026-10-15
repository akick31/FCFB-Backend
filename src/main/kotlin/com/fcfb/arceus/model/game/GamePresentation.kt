package com.fcfb.arceus.model.game

import com.fcfb.arceus.enums.game.TVChannel
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "game_presentation")
class GamePresentation(
    @Id
    @Column(name = "game_id")
    var gameId: Int = 0,
    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "tv_channel")
    var tvChannel: TVChannel? = null,
    @Basic
    @Column(name = "postseason_game_logo")
    var postseasonGameLogo: String? = null,
    @Basic
    @Column(name = "postseason_game_name")
    var postseasonGameName: String? = null,
    @Basic
    @Column(name = "neutral_site", columnDefinition = "tinyint(1)")
    var neutralSite: Boolean = false,
    @Basic
    @Column(name = "venue")
    var venue: String? = null,
)
