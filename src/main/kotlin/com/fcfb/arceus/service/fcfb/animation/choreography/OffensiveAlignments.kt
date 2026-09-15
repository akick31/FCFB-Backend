package com.fcfb.arceus.service.fcfb.animation.choreography

import com.fcfb.arceus.enums.team.OffensivePlaybook

object OffensiveAlignments {
    val LINEMEN = 0..4
    const val LINE_DEPTH = 1.1f
    private const val OFF_LINE_DEPTH = 2f
    private val line = listOf(-4f, -2f, 0f, 2f, 4f).map { Spot(LINE_DEPTH, it) }

    fun forPlaybook(playbook: OffensivePlaybook): OffensiveAlignment =
        when (playbook) {
            OffensivePlaybook.PRO ->
                build(
                    underCenter = true,
                    quarterback = Spot(1.8f, 0f),
                    backs = listOf(Spot(7f, 0f), Spot(4.5f, 0f)),
                    receivers = listOf(Spot(LINE_DEPTH, 6f), Spot(LINE_DEPTH, -18f), Spot(OFF_LINE_DEPTH, 16f)),
                )
            OffensivePlaybook.WEST_COAST ->
                build(
                    underCenter = true,
                    quarterback = Spot(1.8f, 0f),
                    backs = listOf(Spot(6.5f, -1f)),
                    receivers =
                        listOf(Spot(LINE_DEPTH, 6f), Spot(LINE_DEPTH, -17f), Spot(OFF_LINE_DEPTH, 11f), Spot(OFF_LINE_DEPTH, 18f)),
                )
            OffensivePlaybook.FLEXBONE ->
                build(
                    underCenter = false,
                    quarterback = Spot(4.5f, 0f),
                    backs = listOf(Spot(4.5f, -2.5f), Spot(4.5f, 2.5f)),
                    receivers = listOf(Spot(OFF_LINE_DEPTH, 6.5f), Spot(LINE_DEPTH, -18f), Spot(LINE_DEPTH, 18f)),
                )
            OffensivePlaybook.SPREAD ->
                build(
                    underCenter = false,
                    quarterback = Spot(5f, 0f),
                    backs = listOf(Spot(5f, 2.5f)),
                    receivers =
                        listOf(Spot(LINE_DEPTH, -19f), Spot(OFF_LINE_DEPTH, -10f), Spot(OFF_LINE_DEPTH, 10f), Spot(LINE_DEPTH, 19f)),
                )
            OffensivePlaybook.AIR_RAID ->
                build(
                    underCenter = false,
                    quarterback = Spot(5f, 0f),
                    backs = listOf(Spot(5f, -2.5f)),
                    receivers =
                        listOf(Spot(LINE_DEPTH, -19f), Spot(OFF_LINE_DEPTH, -11f), Spot(OFF_LINE_DEPTH, 11f), Spot(LINE_DEPTH, 19f)),
                )
        }

    private fun build(
        underCenter: Boolean,
        quarterback: Spot,
        backs: List<Spot>,
        receivers: List<Spot>,
    ): OffensiveAlignment {
        val quarterbackIndex = line.size
        val firstBack = quarterbackIndex + 1
        val firstReceiver = firstBack + backs.size
        return OffensiveAlignment(
            spots = line + quarterback + backs + receivers,
            quarterback = quarterbackIndex,
            backs = (firstBack until firstReceiver).toList(),
            receivers = (firstReceiver until firstReceiver + receivers.size).toList(),
            underCenter = underCenter,
        )
    }
}
