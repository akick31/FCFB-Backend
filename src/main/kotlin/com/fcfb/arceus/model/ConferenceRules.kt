package com.fcfb.arceus.model

import com.fcfb.arceus.enums.team.Conference
import com.vladmihalcea.hibernate.type.json.JsonStringType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "conference_rules")
@TypeDef(name = "json", typeClass = JsonStringType::class)
class ConferenceRules {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int = 0

    @Enumerated(EnumType.STRING)
    @Basic
    @Column(name = "conference", nullable = false, unique = true)
    lateinit var conference: Conference

    @Basic
    @Column(name = "num_conference_games", nullable = false)
    var numConferenceGames: Int = 9

    @Type(type = "json")
    @Column(name = "protected_rivalries", columnDefinition = "JSON")
    var protectedRivalries: String? = null
}
