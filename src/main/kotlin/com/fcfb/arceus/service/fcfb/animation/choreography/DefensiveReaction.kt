package com.fcfb.arceus.service.fcfb.animation.choreography

object DefensiveReaction {
    private const val RUN_READ_STEP = 1f
    private const val HOOK_DROP = 4f
    private const val SAFETY_BACKPEDAL = 1.5f
    private const val LATE_LINEMAN_REACTION = 0.1f
    private const val CORNER_COUNT = 2

    /** Pre-reaction movement: linemen stay engaged with their blocker, corners mirror their receiver, second level reads. */
    fun before(
        context: PlayContext,
        scene: ScrimmageScene,
        offense: List<Track>,
        dropping: Boolean,
    ): List<Track> {
        val alignment = scene.defensiveAlignment
        val forward = context.forward
        val corners = alignment.secondary.take(CORNER_COUNT)
        return scene.defense.mapIndexed { index, start ->
            when (index) {
                in alignment.linemen ->
                    offense[scene.blockerFor(index)].offsetBy(FieldPoint(forward * 2 * OffensiveAlignments.LINE_DEPTH, 0f))
                in alignment.linebackers ->
                    if (dropping) {
                        path(0f to start, 0.12f to start, 0.4f to start + FieldPoint(forward * HOOK_DROP, 0f))
                    } else {
                        path(0f to start, 0.22f to start + FieldPoint(-forward * RUN_READ_STEP, 0f))
                    }
                in corners -> {
                    val receiver = scene.nearestReceiver(index)
                    offense[receiver].offsetBy(start - scene.offense[receiver])
                }
                else -> path(0f to start, 0.35f to start + FieldPoint(forward * SAFETY_BACKPEDAL, 0f))
            }
        }
    }

    fun respond(
        scene: ScrimmageScene,
        before: List<Track>,
        carrier: Track,
        carrierDirection: Float,
        reactAt: Float,
        tackleAt: Float,
        tacklers: Int,
        linemenCanTackle: Boolean,
    ): List<Track> {
        val alignment = scene.defensiveAlignment
        val candidates = if (linemenCanTackle) scene.defense.indices.toList() else alignment.linebackers + alignment.secondary
        return GangTackle.converge(
            before = before,
            candidates = candidates,
            carrier = carrier,
            direction = carrierDirection,
            tackleAt = tackleAt,
            tacklers = tacklers,
            reactAt = { index -> if (index in alignment.linemen && !linemenCanTackle) reactAt + LATE_LINEMAN_REACTION else reactAt },
            speed = { index ->
                when (index) {
                    in alignment.linemen -> Pursuit.LINEMAN_SPEED
                    in alignment.linebackers -> Pursuit.LINEBACKER_SPEED
                    else -> Pursuit.DEFENSIVE_BACK_SPEED
                }
            },
        )
    }
}
