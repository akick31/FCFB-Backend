package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "venue")
class Venue {
    @Id
    @Column(name = "name")
    lateinit var name: String

    @Basic
    @Column(name = "city")
    var city: String? = null

    @Basic
    @Column(name = "state")
    var state: String? = null

    @Basic
    @Column(name = "capacity")
    var capacity: Int? = null

    constructor(
        name: String,
        city: String? = null,
        state: String? = null,
        capacity: Int? = null,
    ) {
        this.name = name
        this.city = city
        this.state = state
        this.capacity = capacity
    }

    constructor()
}
