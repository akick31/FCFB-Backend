package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "offseason")
class Offseason {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int = 0

    @Basic
    @Column(name = "start_date")
    var startDate: String? = null

    @Basic
    @Column(name = "end_date")
    var endDate: String? = null

    constructor(startDate: String?, endDate: String?) {
        this.startDate = startDate
        this.endDate = endDate
    }

    constructor()
}
