package com.fcfb.arceus.model

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
}
