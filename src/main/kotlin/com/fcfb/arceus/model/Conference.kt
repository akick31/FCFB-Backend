package com.fcfb.arceus.model

import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "conference")
class Conference {
    @Id
    @Column(name = "code")
    lateinit var code: String

    @Basic
    @Column(name = "label")
    lateinit var label: String

    @Basic
    @Column(name = "logo_url")
    var logoUrl: String? = null

    @Basic
    @Column(name = "logo_url_dark")
    var logoUrlDark: String? = null

    @Basic
    @Column(name = "abbreviation")
    var abbreviation: String? = null

    @Basic
    @Column(name = "active", columnDefinition = "tinyint(1)")
    var active: Boolean = true

    @Basic
    @Column(name = "display_order")
    var displayOrder: Int = 0

    constructor(
        code: String,
        label: String,
        logoUrl: String?,
        logoUrlDark: String?,
        abbreviation: String?,
        active: Boolean,
        displayOrder: Int,
    ) {
        this.code = code
        this.label = label
        this.logoUrl = logoUrl
        this.logoUrlDark = logoUrlDark
        this.abbreviation = abbreviation
        this.active = active
        this.displayOrder = displayOrder
    }

    constructor()
}
