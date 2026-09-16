package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.choreography.Spot

internal object PuntFormation {
    val GUNNERS = listOf(7, 8)
    const val PERSONAL_PROTECTOR = 9
    const val KICKER = 10
    const val PUNT_DEPTH = 14f
    const val FIELD_GOAL_DEPTH = 7f

    private const val LINE_DEPTH = 1.1f
    private const val BACK_DEPTH = 5.5f

    /** Gunners split out near the numbers, wide enough that the camera crops them until the play comes their way. */
    private const val GUNNER_WIDTH = 24f

    val RUSHERS = listOf(-9f, -5f, -1.5f, 1.5f, 5f, 9f).map { Spot(LINE_DEPTH, it) }
    val JAMMERS = listOf(Spot(LINE_DEPTH, -GUNNER_WIDTH + 1f), Spot(LINE_DEPTH, GUNNER_WIDTH - 1f))
    val MIDDLE = listOf(Spot(6f, -6f), Spot(6f, 6f))

    /** Tackle to tackle with tight splits, two gunners split out near the numbers, and three backs shielding the punter. */
    fun kickingTeam(kickerDepth: Float): List<Spot> =
        listOf(
            Spot(LINE_DEPTH, -5f),
            Spot(LINE_DEPTH, -2.5f),
            Spot(LINE_DEPTH, 0f),
            Spot(LINE_DEPTH, 2.5f),
            Spot(LINE_DEPTH, 5f),
            Spot(BACK_DEPTH, -5f),
            Spot(BACK_DEPTH, 5f),
            Spot(LINE_DEPTH, -GUNNER_WIDTH),
            Spot(LINE_DEPTH, GUNNER_WIDTH),
            Spot(BACK_DEPTH, 0f),
            Spot(kickerDepth, 0f),
        )
}
