package com.fcfb.arceus.model.play

import com.fcfb.arceus.enums.play.Scenario
import java.util.Objects
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "ranges")
class Ranges {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    var id: Int? = 0

    @Basic
    @Column(name = "play_type")
    var playType: String? = null

    @Basic
    @Column(name = "offensive_playbook")
    var offensivePlaybook: String? = null

    @Basic
    @Column(name = "defensive_playbook")
    var defensivePlaybook: String? = null

    @Basic
    @Column(name = "ball_location_lower")
    var ballLocationLower: Int? = null

    @Basic
    @Column(name = "ball_location_upper")
    var ballLocationUpper: Int? = null

    @Basic
    @Column(name = "distance")
    var distance: Int? = null

    @Basic
    @Column(name = "result")
    var result: Scenario? = null

    @Basic
    @Column(name = "play_time")
    var playTime: Int = 0

    @Basic
    @Column(name = "lower_range")
    var lowerRange: Int? = null

    @Basic
    @Column(name = "upper_range")
    var upperRange: Int? = null

    constructor(
        playType: String?,
        offensivePlaybook: String?,
        defensivePlaybook: String?,
        ballLocationLower: Int?,
        ballLocationUpper: Int?,
        distance: Int?,
        result: Scenario?,
        playTime: Int,
        lowerRange: Int?,
        upperRange: Int?,
    ) {
        this.playType = playType
        this.offensivePlaybook = offensivePlaybook
        this.defensivePlaybook = defensivePlaybook
        this.ballLocationLower = ballLocationLower
        this.ballLocationUpper = ballLocationUpper
        this.distance = distance
        this.result = result
        this.playTime = playTime
        this.lowerRange = lowerRange
        this.upperRange = upperRange
    }

    constructor()

    override fun hashCode(): Int {
        return Objects.hash(
            id,
            playType,
            offensivePlaybook,
            defensivePlaybook,
            ballLocationLower,
            ballLocationUpper,
            distance,
            result,
            playTime,
            lowerRange,
            upperRange,
        )
    }
}
