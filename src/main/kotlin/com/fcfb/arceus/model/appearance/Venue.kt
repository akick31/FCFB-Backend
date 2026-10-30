package com.fcfb.arceus.model.appearance

import org.hibernate.annotations.Generated
import org.hibernate.annotations.GenerationTime
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "venue")
class Venue {
    @Id
    @Column(name = "name")
    lateinit var name: String

    @Generated(GenerationTime.INSERT)
    @Column(name = "id", insertable = false, updatable = false)
    var venueId: Long? = null

    @Basic
    @Column(name = "city")
    var city: String? = null

    @Basic
    @Column(name = "state")
    var state: String? = null

    @Basic
    @Column(name = "country")
    var country: String = DEFAULT_COUNTRY

    @Basic
    @Column(name = "capacity")
    var capacity: Int? = null

    @Basic
    @Column(name = "active")
    var active: Boolean = true

    @Embedded
    var design: StadiumDesign = StadiumDesign()

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

    companion object {
        const val DEFAULT_COUNTRY = "United States"
    }
}
