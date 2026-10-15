package com.fcfb.arceus.model.account

import com.vladmihalcea.hibernate.type.json.JsonStringType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "user_preferences")
@TypeDef(name = "json", typeClass = JsonStringType::class)
class UserPreferences(
    @Id
    @Column(name = "user_id")
    var userId: Long = 0,
    @Basic
    @Column(name = "delay_of_game_warning_opt_out", columnDefinition = "tinyint(1)")
    var delayOfGameWarningOptOut: Boolean = false,
    @Type(type = "json")
    @Column(name = "scouting_report_hidden_columns", columnDefinition = "longtext")
    var scoutingReportHiddenColumns: MutableList<String>? = mutableListOf(),
)
