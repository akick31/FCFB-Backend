package com.fcfb.arceus.model

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "conference_championship_field")
class ConferenceChampionshipField : PostseasonField() {
    @Id
    @Column(name = "conference")
    lateinit var conference: String
}
