package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "bowl_venue")
class BowlVenue {
    @Id
    @Column(name = "name")
    lateinit var name: String

    @Basic
    @Column(name = "venue")
    var venue: String? = null

    constructor(name: String, venue: String?) {
        this.name = name
        this.venue = venue
    }

    constructor()
}
