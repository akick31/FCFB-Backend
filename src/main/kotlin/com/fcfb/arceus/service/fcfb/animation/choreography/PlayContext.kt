package com.fcfb.arceus.service.fcfb.animation.choreography

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.model.Play

data class PlayContext(
    val play: Play,
    val lineOfScrimmage: Float,
    val endSpot: Float,
    val forward: Float,
    val side: Float,
    val offensivePlaybook: OffensivePlaybook,
    val defensivePlaybook: DefensivePlaybook,
) {
    val gain: Float get() = (endSpot - lineOfScrimmage) * forward

    val endsInEndZone: Boolean get() = endSpot < 0f || endSpot > 100f

    fun offenseSpot(
        depth: Float,
        lateral: Float,
    ) = FieldPoint(lineOfScrimmage - forward * depth, lateral)

    fun defenseSpot(
        depth: Float,
        lateral: Float,
    ) = FieldPoint(lineOfScrimmage + forward * depth, lateral)
}
