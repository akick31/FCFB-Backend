package com.fcfb.arceus.service.fcfb.animation.choreography.script

import com.fcfb.arceus.service.fcfb.animation.PlayRandom
import com.fcfb.arceus.service.fcfb.animation.choreography.BallState
import com.fcfb.arceus.service.fcfb.animation.choreography.BallTrack
import com.fcfb.arceus.service.fcfb.animation.choreography.FieldPoint
import com.fcfb.arceus.service.fcfb.animation.choreography.OffensiveAlignments
import com.fcfb.arceus.service.fcfb.animation.choreography.PlayContext
import com.fcfb.arceus.service.fcfb.animation.choreography.SNAP_END
import com.fcfb.arceus.service.fcfb.animation.choreography.ScrimmageScene
import com.fcfb.arceus.service.fcfb.animation.choreography.Track
import com.fcfb.arceus.service.fcfb.animation.choreography.UNDER_CENTER_SNAP_SPEED
import com.fcfb.arceus.service.fcfb.animation.choreography.arc
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.snapBall
import kotlin.math.abs

internal class PassConcept(
    private val context: PlayContext,
    private val scene: ScrimmageScene,
) {
    private val alignment = scene.offensiveAlignment
    private val forward = context.forward
    private val quarterbackStart = scene.offense[alignment.quarterback]
    private val center = context.offenseSpot(0.3f, 0f)

    val dropEnd = if (alignment.underCenter) UNDER_CENTER_DROP_END else SHOTGUN_DROP_END
    val throwAt = dropEnd + SET_TIME
    val setPoint =
        context.offenseSpot(
            if (alignment.underCenter) UNDER_CENTER_DROP else alignment.spots[alignment.quarterback].depth + SHOTGUN_DROP,
            0f,
        )
    val release = setPoint + carryOffset(forward)

    private val exchangeEnd = if (alignment.underCenter) SNAP_END * UNDER_CENTER_SNAP_SPEED else SNAP_END

    fun quarterback(vararg after: Pair<Float, FieldPoint>): Track =
        path(0f to quarterbackStart, exchangeEnd to quarterbackStart, dropEnd to setPoint, *after)

    fun throwingQuarterback(): Track =
        quarterback(
            throwAt to setPoint,
            throwAt + FOLLOW_THROUGH_TIME to setPoint + FieldPoint(forward * 0.8f, 0f),
        )

    fun offense(
        quarterback: Track,
        overrides: Map<Int, Track>,
    ): List<Track> =
        scene.offense.mapIndexed { index, start ->
            overrides[index] ?: when {
                index in OffensiveAlignments.LINEMEN ->
                    path(0f to start, 0.1f to start, 0.3f to start + FieldPoint(-forward * PASS_SET, 0f))
                index == alignment.quarterback -> quarterback
                index in alignment.backs ->
                    path(0f to start, 0.12f to start, 0.3f to context.offenseSpot(PROTECTION_DEPTH, if (start.lateral >= 0f) 2f else -2f))
                else -> route(index)
            }
        }

    fun target(
        deep: Boolean,
        random: PlayRandom,
    ): Int {
        val ordered = alignment.receivers.sortedBy { abs(scene.offense[it].lateral) }
        val half = maxOf(1, ordered.size / 2)
        return random.pick(if (deep) ordered.takeLast(half) else ordered.take(half))
    }

    fun routeTo(
        index: Int,
        catchMarker: FieldPoint,
        catchAt: Float,
    ): Track {
        val start = scene.offense[index]
        if ((catchMarker.along - context.lineOfScrimmage) * forward < 2f) {
            return path(
                0f to start,
                SNAP_END to start,
                catchAt to catchMarker,
            )
        }
        val stemAt = maxOf(SNAP_END + 0.05f, catchAt - BREAK_TIME)
        return path(0f to start, SNAP_END to start, stemAt to FieldPoint(catchMarker.along, start.lateral), catchAt to catchMarker)
    }

    fun flightTime(catchPoint: FieldPoint): Float = BASE_FLIGHT + DISTANCE_FLIGHT * minOf(release.distanceTo(catchPoint), 45f) / 45f

    fun heldBall(quarterback: Track): BallTrack =
        BallTrack { progress ->
            if (progress < SNAP_END) {
                snapBall(progress, center, quarterback.at(SNAP_END) + carryOffset(forward), !alignment.underCenter)
            } else {
                BallState(quarterback.at(progress) + carryOffset(forward))
            }
        }

    fun thrown(
        catchPoint: FieldPoint,
        fraction: Float,
    ): BallState =
        BallState(
            release.lerp(catchPoint, fraction),
            arc(fraction, 1.5f + release.distanceTo(catchPoint) * DEEP_ARC_PER_YARD),
            spinning = true,
        )

    private fun route(index: Int): Track {
        val start = scene.offense[index]
        val order = alignment.receivers.indexOf(index)
        val depth = 10f + (order % 3) * 4f
        val outward = if (start.lateral >= 0f) 1f else -1f
        return path(
            0f to start,
            SNAP_END to start,
            0.5f to FieldPoint(context.lineOfScrimmage + forward * depth, start.lateral + outward * 2f),
            0.85f to FieldPoint(context.lineOfScrimmage + forward * (depth + 5f), start.lateral * 0.6f),
        )
    }

    companion object {
        private const val UNDER_CENTER_DROP_END = 0.28f
        private const val SHOTGUN_DROP_END = 0.2f
        private const val SET_TIME = 0.03f
        private const val FOLLOW_THROUGH_TIME = 0.08f
        private const val UNDER_CENTER_DROP = 7f
        private const val SHOTGUN_DROP = 1.5f
        private const val PASS_SET = 1.2f
        private const val PROTECTION_DEPTH = 3.5f
        private const val BREAK_TIME = 0.12f
        private const val DEEP_ARC_PER_YARD = 0.42f

        private const val BASE_FLIGHT = 0.05f
        private const val DISTANCE_FLIGHT = 0.24f
    }
}
