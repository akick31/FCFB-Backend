package com.fcfb.arceus.model

import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
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
@Table(name = "records")
class Record(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "record_name", nullable = false)
    var recordName: Stats = Stats.SCORE,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false)
    var recordType: RecordType = RecordType.SINGLE_GAME,
    
    @Basic
    @Column(name = "season_number", nullable = false)
    var seasonNumber: Int = 0,
    
    @Basic
    @Column(name = "week")
    var week: Int? = null,
    
    @Basic
    @Column(name = "game_id")
    var gameId: Int? = null,
    
    @Basic
    @Column(name = "home_team")
    var homeTeam: String? = null,
    
    @Basic
    @Column(name = "away_team")
    var awayTeam: String? = null,
    
    @Basic
    @Column(name = "record_team", nullable = false)
    var recordTeam: String = "",
    
    @Basic
    @Column(name = "coach")
    var coach: String? = null,
    
    @Basic
    @Column(name = "record_value", nullable = false)
    var recordValue: Double = 0.0,
    
    @Basic
    @Column(name = "previous_record_value")
    var previousRecordValue: Double? = null,
    
    @Basic
    @Column(name = "previous_record_team")
    var previousRecordTeam: String? = null,
    
    @Basic
    @Column(name = "previous_record_game_id")
    var previousRecordGameId: Int? = null,
    
    @Basic
    @Column(name = "is_tied")
    var isTied: Boolean = false,
    
    @Basic
    @Column(name = "tied_teams")
    var tiedTeams: String? = null,
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    var createdAt: LocalDateTime? = null,
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
) {
    // Default constructor for JPA
    constructor() : this(
        id = 0,
        recordName = Stats.SCORE,
        recordType = RecordType.SINGLE_GAME,
        seasonNumber = 0,
        week = null,
        gameId = null,
        homeTeam = null,
        awayTeam = null,
        recordTeam = "",
        coach = null,
        recordValue = 0.0,
        previousRecordValue = null,
        previousRecordTeam = null,
        previousRecordGameId = null,
        isTied = false,
        tiedTeams = null,
        createdAt = null,
        updatedAt = null
    )
}
