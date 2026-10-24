package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop.facility

object LowBuildingLayout {
    const val CENTER_HALF = 270
    const val WING_END = 660
    const val PAVILION_END = 780
    const val PAVILION_CENTER = (WING_END + PAVILION_END) / 2
    const val PAVILION_HALF = (PAVILION_END - WING_END) / 2
    private const val PAST_EDGE = 40

    fun outerEnd(frame: FacilityFrame): Int = maxOf(frame.rightEdgeReference() + PAST_EDGE, PAVILION_END)

    fun repeating(
        first: Int,
        pitch: Int,
        until: Int,
        margin: Int,
    ): List<Int> = generateSequence(first) { it + pitch }.takeWhile { it < until - margin }.flatMap { listOf(it, -it) }.toList()

    fun mirrored(
        from: Int,
        to: Int,
    ): List<Pair<Int, Int>> = listOf(from to to, -to to -from)
}
