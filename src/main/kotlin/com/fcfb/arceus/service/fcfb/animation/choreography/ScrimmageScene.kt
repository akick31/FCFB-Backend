package com.fcfb.arceus.service.fcfb.animation.choreography

import kotlin.math.abs

data class ScrimmageScene(
    val offense: List<FieldPoint>,
    val defense: List<FieldPoint>,
    val offensiveAlignment: OffensiveAlignment,
    val defensiveAlignment: DefensiveAlignment,
) {
    fun blockerFor(defender: Int): Int = OffensiveAlignments.LINEMEN.minBy { abs(offense[it].lateral - defense[defender].lateral) }

    fun nearestReceiver(defender: Int): Int =
        offensiveAlignment.receivers.minByOrNull { abs(offense[it].lateral - defense[defender].lateral) }
            ?: offensiveAlignment.quarterback

    companion object {
        /** [alignment] overrides the playbook for plays whose formation is fixed, such as a kneel or a spike. */
        fun from(
            context: PlayContext,
            alignment: OffensiveAlignment? = null,
        ): ScrimmageScene {
            val offensive = alignment ?: OffensiveAlignments.forPlaybook(context.offensivePlaybook)
            val defensive = DefensiveAlignments.forPlaybook(context.defensivePlaybook, offensive)
            return ScrimmageScene(
                offense = offensive.spots.map { context.offenseSpot(it.depth, it.lateral) },
                defense = defensive.spots.map { context.defenseSpot(it.depth, it.lateral) },
                offensiveAlignment = offensive,
                defensiveAlignment = defensive,
            )
        }
    }
}
