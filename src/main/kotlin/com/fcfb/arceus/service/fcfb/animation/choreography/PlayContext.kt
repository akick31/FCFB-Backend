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
    ) = keepInField(FieldPoint(lineOfScrimmage + forward * depth, lateral))

    /**
     * Near the goal line a defender's alignment depth would otherwise place them behind the back of the end zone. This
     * pulls them up so they stand no deeper than [MAX_END_ZONE_DEPTH] yards into the end zone they are defending.
     */
    fun keepInField(point: FieldPoint): FieldPoint {
        val deepest = defendedGoal + forward * MAX_END_ZONE_DEPTH
        val along = if ((point.along - deepest) * forward > 0f) deepest else point.along
        return FieldPoint(along, point.lateral)
    }

    private val defendedGoal: Float get() = if (forward > 0f) 100f else 0f

    companion object {
        private const val MAX_END_ZONE_DEPTH = 3f
    }
}
