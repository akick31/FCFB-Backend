package com.fcfb.arceus.model.appearance

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "playoff_field")
class PlayoffField : PostseasonField() {
    @Id
    @Column(name = "round")
    lateinit var round: String

    @Basic
    @Column(name = "venue")
    var venue: String? = null
}
