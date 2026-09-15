package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.choreography.Spot

internal object PuntFormation {
    val GUNNERS = listOf(7, 8)
    const val PERSONAL_PROTECTOR = 9
    const val KICKER = 10
    const val PUNT_DEPTH = 14f
    const val FIELD_GOAL_DEPTH = 7f

    val RUSHERS = listOf(-9f, -5f, -1.5f, 1.5f, 5f, 9f).map { Spot(1.1f, it) }
    val JAMMERS = listOf(Spot(1.1f, -20.5f), Spot(1.1f, 20.5f))
    val MIDDLE = listOf(Spot(10f, -6f), Spot(10f, 6f))

    fun kickingTeam(kickerDepth: Float): List<Spot> =
        listOf(
            Spot(1.1f, -4f),
            Spot(1.1f, -2f),
            Spot(1.1f, 0f),
            Spot(1.1f, 2f),
            Spot(1.1f, 4f),
            Spot(2.2f, -5.5f),
            Spot(2.2f, 5.5f),
            Spot(1.1f, -22f),
            Spot(1.1f, 22f),
            Spot(6f, 0f),
            Spot(kickerDepth, 0f),
        )
}
