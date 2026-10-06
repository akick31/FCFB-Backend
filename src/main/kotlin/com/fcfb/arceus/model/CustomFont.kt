package com.fcfb.arceus.model

import java.time.LocalDateTime
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/** A user-contributed display font, referenced by URL and registered with the graphics environment at load. */
@Entity
@Table(name = "custom_font")
class CustomFont {
    @Id
    @Column(name = "name")
    lateinit var name: String

    @Basic
    @Column(name = "label")
    lateinit var label: String

    @Basic
    @Column(name = "family")
    lateinit var family: String

    @Basic
    @Column(name = "url")
    lateinit var url: String

    @Basic
    @Column(name = "uploaded_by")
    var uploadedBy: Long? = null

    @Basic
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null
}
