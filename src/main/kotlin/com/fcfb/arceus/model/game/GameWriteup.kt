package com.fcfb.arceus.model.game

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "game_writeup")
class GameWriteup {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    var id = 0

    @Basic
    @Column(name = "scenario")
    var scenario: String? = null

    @Basic
    @Column(name = "play_call")
    var playCall: String? = null

    @Basic
    @Column(name = "message")
    var message: String? = null

    constructor(
        scenario: String?,
        playCall: String?,
        message: String?,
    ) {
        this.scenario = scenario
        this.playCall = playCall
        this.message = message
    }

    constructor()
}
