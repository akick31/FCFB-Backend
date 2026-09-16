package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.model.Play
import java.util.Random

/**
 * Picks the variation shown for a play. The seed comes from the play's own identity, so the same play always animates the
 * same way without storing anything: re-rendering a historical play years later replays the same scenario.
 */
class PlayRandom(play: Play) {
    private val random = Random(scramble(play.gameId.toLong() * GAME_STRIDE + play.playId))

    fun fraction(): Float = random.nextFloat()

    fun chance(probability: Float): Boolean = random.nextFloat() < probability

    fun side(): Float = if (random.nextBoolean()) 1f else -1f

    fun between(
        from: Float,
        to: Float,
    ): Float = from + random.nextFloat() * (to - from)

    fun <T> pick(options: List<T>): T = options[random.nextInt(options.size)]

    private companion object {
        const val GAME_STRIDE = 1_000_003L

        /**
         * Consecutive seeds give java.util.Random almost the same first value, so without this every play in a game drew
         * the same opening fraction and the first branch of a script was decided by the game rather than the play.
         */
        fun scramble(seed: Long): Long {
            var z = seed + -0x61c8864680b583ebL
            z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
            z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
            return z xor (z ushr 31)
        }
    }
}
