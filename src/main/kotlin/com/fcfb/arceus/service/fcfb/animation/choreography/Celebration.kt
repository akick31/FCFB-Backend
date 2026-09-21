package com.fcfb.arceus.service.fcfb.animation.choreography

object Celebration {
    private const val MOB_RADIUS = 1.6f
    private const val MOB_STEP = 0.9f
    private const val MOB_SPEED = 70f

    private const val MOB_LEAD = 0.25f
    private const val SPIKE_HEIGHT = 2.2f
    private const val SPIKE_BOUNCES = 3

    fun after(
        choreography: Choreography,
        scoreAt: Float,
    ): Choreography {
        val spot = choreography.ball.at(scoreAt).position
        val offenseScored = nearest(choreography.offense, spot, scoreAt) <= nearest(choreography.defense, spot, scoreAt)
        val scoring = if (offenseScored) choreography.offense else choreography.defense
        val scorerIndex = scoring.indices.minBy { scoring[it].at(scoreAt).distanceTo(spot) }
        val scorer = scoring[scorerIndex]
        val escortFrom = maxOf(0f, scoreAt - MOB_LEAD)
        val mobbed =
            scoring.mapIndexed { index, track ->
                if (index == scorerIndex) {
                    track
                } else {
                    Pursuit.chase(track, escortFrom, MOB_SPEED, Pursuit.trail(scorer, MOB_RADIUS + (index % 3) * MOB_STEP))
                }
            }
        val ball = spiked(choreography.ball, scoreAt)
        return if (offenseScored) choreography.copy(offense = mobbed, ball = ball) else choreography.copy(defense = mobbed, ball = ball)
    }

    private fun nearest(
        tracks: List<Track>,
        spot: FieldPoint,
        scoreAt: Float,
    ): Float = tracks.minOf { it.at(scoreAt).distanceTo(spot) }

    private fun spiked(
        ball: BallTrack,
        scoreAt: Float,
    ): BallTrack =
        BallTrack { progress ->
            if (progress < scoreAt) {
                ball.at(progress)
            } else {
                val landed = ball.at(scoreAt)
                BallState(landed.position, bounce(segment(progress, scoreAt, 1f), SPIKE_HEIGHT, SPIKE_BOUNCES), tumbling = true)
            }
        }
}
