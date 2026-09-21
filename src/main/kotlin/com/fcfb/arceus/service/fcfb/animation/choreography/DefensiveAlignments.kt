package com.fcfb.arceus.service.fcfb.animation.choreography

import com.fcfb.arceus.enums.team.DefensivePlaybook
import kotlin.math.abs

object DefensiveAlignments {
    private const val LINE_DEPTH = OffensiveAlignments.LINE_DEPTH
    private const val STACKED_DEPTH = 4.5f
    private const val EDGE_DEPTH = 2.5f
    private const val CORNER_DEPTH = 6f
    private const val NICKEL_DEPTH = 5f
    private const val SAFETY_DEPTH = 12f
    private const val SAFETY_WIDTH = 8f
    private const val INSIDE_LEVERAGE = 1f

    fun forPlaybook(
        playbook: DefensivePlaybook,
        offense: OffensiveAlignment,
    ): DefensiveAlignment {
        val (lineLaterals, linebackers, defensiveBacks) =
            when (playbook) {
                DefensivePlaybook.FOUR_THREE -> Triple(listOf(-4f, -2f, 2f, 4f), stack(-4f, 0f, 4f), 4)
                DefensivePlaybook.THREE_FOUR -> Triple(listOf(-4f, 0f, 4f), edgesAndStack(), 4)
                DefensivePlaybook.FIVE_TWO -> Triple(listOf(-4f, -2f, 0f, 2f, 4f), stack(-2.5f, 2.5f), 4)
                DefensivePlaybook.FOUR_FOUR -> Triple(listOf(-4f, -2f, 2f, 4f), edgesAndStack(), 3)
                DefensivePlaybook.THREE_THREE_FIVE -> Triple(listOf(-4f, 0f, 4f), stack(-4f, 0f, 4f), 5)
            }
        val line = lineLaterals.map { Spot(LINE_DEPTH, it) }
        val receiverLaterals = offense.receivers.map { offense.spots[it].lateral }
        val secondary = secondary(defensiveBacks, receiverLaterals)
        val firstLinebacker = line.size
        val firstBack = firstLinebacker + linebackers.size
        return DefensiveAlignment(
            spots = line + linebackers + secondary,
            linemen = line.indices.toList(),
            linebackers = (firstLinebacker until firstBack).toList(),
            secondary = (firstBack until firstBack + secondary.size).toList(),
        )
    }

    private fun stack(vararg laterals: Float) = laterals.map { Spot(STACKED_DEPTH, it) }

    private fun edgesAndStack() = listOf(Spot(EDGE_DEPTH, -7f), Spot(STACKED_DEPTH, -2.5f), Spot(STACKED_DEPTH, 2.5f), Spot(EDGE_DEPTH, 7f))

    private fun secondary(
        count: Int,
        receiverLaterals: List<Float>,
    ): List<Spot> {
        val widestLow = receiverLaterals.filter { it < 0f }.minOrNull() ?: -14f
        val widestHigh = receiverLaterals.filter { it > 0f }.maxOrNull() ?: 14f
        val corners = listOf(Spot(CORNER_DEPTH, widestLow + INSIDE_LEVERAGE), Spot(CORNER_DEPTH, widestHigh - INSIDE_LEVERAGE))
        val slot = receiverLaterals.firstOrNull { it != widestLow && it != widestHigh && abs(it) > 7f } ?: 9f
        return when (count) {
            3 -> corners + Spot(SAFETY_DEPTH + 1f, 0f)
            4 -> corners + Spot(SAFETY_DEPTH, -SAFETY_WIDTH) + Spot(SAFETY_DEPTH, SAFETY_WIDTH)
            else -> corners + Spot(SAFETY_DEPTH, -SAFETY_WIDTH) + Spot(SAFETY_DEPTH, SAFETY_WIDTH) + Spot(NICKEL_DEPTH, slot)
        }
    }
}
