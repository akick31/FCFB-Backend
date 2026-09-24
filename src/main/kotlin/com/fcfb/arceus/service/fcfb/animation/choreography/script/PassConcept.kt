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
import com.fcfb.arceus.service.fcfb.animation.choreography.arriveBy
import com.fcfb.arceus.service.fcfb.animation.choreography.carryOffset
import com.fcfb.arceus.service.fcfb.animation.choreography.path
import com.fcfb.arceus.service.fcfb.animation.choreography.segment
import com.fcfb.arceus.service.fcfb.animation.choreography.snapBall
import kotlin.math.abs

internal class PassConcept(
    private val context: PlayContext,
    private val scene: ScrimmageScene,
    private val fleaFlicker: Boolean = false,
) {
    private val alignment = scene.offensiveAlignment
    private val forward = context.forward
    private val quarterbackStart = scene.offense[alignment.quarterback]
    private val center = context.offenseSpot(0.3f, 0f)

    val dropEnd =
        when {
            fleaFlicker -> FLEA_SET_END
            alignment.underCenter -> UNDER_CENTER_DROP_END
            else -> SHOTGUN_DROP_END
        }
    val throwAt = dropEnd + SET_TIME
    val setPoint =
        context.offenseSpot(
            if (alignment.underCenter) UNDER_CENTER_DROP else alignment.spots[alignment.quarterback].depth + SHOTGUN_DROP,
            0f,
        )
    val release = setPoint + carryOffset(forward)

    private val exchangeEnd = if (alignment.underCenter) SNAP_END * UNDER_CENTER_SNAP_SPEED else SNAP_END

    private val fleaMeshDepth = if (alignment.underCenter) FLEA_MESH_DEPTH else alignment.spots[alignment.quarterback].depth

    private val fleaMesh = context.offenseSpot(fleaMeshDepth, context.side * FLEA_MESH_WIDTH)

    private val fleaCarry = context.offenseSpot(FLEA_CARRY_DEPTH, context.side * FLEA_CARRY_WIDTH)

    fun quarterback(vararg after: Pair<Float, FieldPoint>): Track =
        if (fleaFlicker) {
            path(0f to quarterbackStart, exchangeEnd to quarterbackStart, FLEA_HANDOFF to fleaMesh, dropEnd to setPoint, *after)
        } else {
            path(0f to quarterbackStart, exchangeEnd to quarterbackStart, dropEnd to setPoint, *after)
        }

    fun fleaBack(index: Int): Track {
        val start = scene.offense[index]
        return path(
            0f to start,
            exchangeEnd to start,
            FLEA_HANDOFF to fleaMesh,
            FLEA_PITCH_AT to fleaCarry,
            dropEnd to fleaCarry - FieldPoint(forward * FLEA_RETREAT, 0f),
        )
    }

    fun fleaHeldBall(
        quarterback: Track,
        back: Track,
    ): BallTrack =
        BallTrack { progress ->
            when {
                progress < SNAP_END ->
                    snapBall(progress, center, quarterback.at(SNAP_END) + carryOffset(forward), !alignment.underCenter)
                progress < FLEA_HANDOFF -> BallState(quarterback.at(progress) + carryOffset(forward))
                progress < FLEA_PITCH_AT -> BallState(back.at(progress) + carryOffset(forward))
                progress < dropEnd -> {
                    val fraction = segment(progress, FLEA_PITCH_AT, dropEnd)
                    BallState(back.at(FLEA_PITCH_AT).lerp(release, fraction), arc(fraction, FLEA_PITCH_HEIGHT), spinning = true)
                }
                else -> BallState(quarterback.at(progress) + carryOffset(forward))
            }
        }

    fun throwingQuarterback(): Track =
        quarterback(
            throwAt to setPoint,
            throwAt + FOLLOW_THROUGH_TIME to setPoint + FieldPoint(forward * 0.8f, 0f),
        )

    fun offense(
        quarterback: Track,
        overrides: Map<Int, Track>,
        conceptDepth: Float = 0f,
    ): List<Track> =
        scene.offense.mapIndexed { index, start ->
            overrides[index] ?: when {
                index in OffensiveAlignments.LINEMEN ->
                    path(0f to start, 0.1f to start, 0.3f to start + FieldPoint(-forward * PASS_SET, 0f))
                index == alignment.quarterback -> quarterback
                index in alignment.backs ->
                    path(0f to start, 0.12f to start, 0.3f to context.offenseSpot(PROTECTION_DEPTH, if (start.lateral >= 0f) 2f else -2f))
                else -> route(index, conceptDepth)
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

    /**
     * A deflected throw: a clean arc from the release to [tipPoint], then a flatter, tumbling
     * deviation on to [endPoint]. [TIP_AT] is the share of the flight spent before contact.
     */
    fun tipped(
        tipPoint: FieldPoint,
        endPoint: FieldPoint,
        fraction: Float,
    ): BallState {
        if (fraction < TIP_AT) {
            val toTip = segment(fraction, 0f, TIP_AT)
            return BallState(
                release.lerp(tipPoint, toTip),
                arc(toTip, 1.5f + release.distanceTo(tipPoint) * DEEP_ARC_PER_YARD),
                spinning = true,
            )
        }
        val afterTip = segment(fraction, TIP_AT, 1f)
        return BallState(
            tipPoint.lerp(endPoint, afterTip),
            arc(afterTip, TIP_POP) * (1f - afterTip),
            tumbling = true,
        )
    }

    fun tipPointFor(
        catchPoint: FieldPoint,
        lateral: Float,
    ): FieldPoint = release.lerp(catchPoint, TIP_AT) + FieldPoint(0f, lateral)

    private fun route(
        index: Int,
        conceptDepth: Float,
    ): Track {
        val start = scene.offense[index]
        val order = alignment.receivers.indexOf(index)
        val short = 10f + (order % 3) * 4f
        val vertical = conceptDepth >= VERTICAL_CONCEPT_DEPTH
        val depth = if (vertical) conceptDepth * (VERTICAL_SHARE - (order % 3) * VERTICAL_STAGGER) else short
        val outward = if (start.lateral >= 0f) 1f else -1f
        val breakLateral = if (vertical) start.lateral + outward * SIDELINE_STEM else start.lateral * 0.6f
        val stem = FieldPoint(context.lineOfScrimmage + forward * depth * STEM_SHARE, start.lateral + outward * 2f)
        val breakPoint = FieldPoint(context.lineOfScrimmage + forward * depth, breakLateral)
        val stemAt = arriveBy(SNAP_END, ROUTE_STEM_AT, start.distanceTo(stem))
        val breakAt = arriveBy(stemAt, ROUTE_BREAK_AT, stem.distanceTo(breakPoint))
        return path(0f to start, SNAP_END to start, stemAt to stem, breakAt to breakPoint)
    }

    companion object {
        private const val FLEA_SET_END = 0.44f
        private const val FLEA_HANDOFF = 0.16f
        private const val FLEA_PITCH_AT = 0.3f
        private const val FLEA_MESH_DEPTH = 4f
        private const val FLEA_MESH_WIDTH = 1.2f
        private const val FLEA_CARRY_DEPTH = 1.5f
        private const val FLEA_CARRY_WIDTH = 3f
        private const val FLEA_RETREAT = 1.5f
        private const val FLEA_PITCH_HEIGHT = 2.2f

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
        private const val VERTICAL_CONCEPT_DEPTH = 25f
        private const val VERTICAL_SHARE = 0.95f
        private const val VERTICAL_STAGGER = 0.12f
        private const val SIDELINE_STEM = 3f
        private const val STEM_SHARE = 0.55f
        private const val ROUTE_STEM_AT = 0.5f
        private const val ROUTE_BREAK_AT = 0.85f

        private const val TIP_AT = 0.68f
        private const val TIP_POP = 2.6f

        private const val BASE_FLIGHT = 0.05f
        private const val DISTANCE_FLIGHT = 0.24f
    }
}
