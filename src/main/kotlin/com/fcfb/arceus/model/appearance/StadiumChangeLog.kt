package com.fcfb.arceus.model.appearance

import com.fcfb.arceus.enums.animation.StadiumChangeAction
import java.time.LocalDateTime
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
@Table(name = "stadium_change_log")
class StadiumChangeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null

    @Basic
    @Column(name = "venue")
    lateinit var venue: String

    @Enumerated(EnumType.STRING)
    @Column(name = "action")
    var action: StadiumChangeAction = StadiumChangeAction.UPDATE

    @Basic
    @Column(name = "summary")
    var summary: String? = null

    @Basic
    @Column(name = "changed_by")
    var changedBy: String? = null

    @Basic
    @Column(name = "changed_at")
    var changedAt: LocalDateTime = LocalDateTime.now()

    @Basic
    @Column(name = "before_json", columnDefinition = "LONGTEXT")
    var beforeJson: String? = null
}
