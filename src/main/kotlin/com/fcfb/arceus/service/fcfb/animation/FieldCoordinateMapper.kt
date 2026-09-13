package com.fcfb.arceus.service.fcfb.animation

import com.fcfb.arceus.enums.team.TeamSide

/** Fixed frame: 0 = HOME's own goal line, 100 = AWAY's own goal line, regardless of who has the ball. */
object FieldCoordinateMapper {
    fun toAbsoluteFieldPosition(
        ballLocation: Int,
        possession: TeamSide,
    ): Int = if (possession == TeamSide.HOME) ballLocation else 100 - ballLocation

    fun toPixelX(
        absolutePosition: Int,
        fieldWidthPx: Int,
        marginPx: Int,
    ): Int {
        val playableWidth = fieldWidthPx - 2 * marginPx
        return marginPx + (absolutePosition * playableWidth / 100)
    }
}
