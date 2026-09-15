package com.fcfb.arceus.service.fcfb.animation.choreography

class WaypointTrack(
    waypoints: List<Waypoint>,
) : Track {
    private val sorted = waypoints.sortedBy { it.progress }

    override fun at(progress: Float): FieldPoint {
        if (progress <= sorted.first().progress) return sorted.first().point
        if (progress >= sorted.last().progress) return sorted.last().point
        val nextIndex = sorted.indexOfFirst { it.progress > progress }
        val from = sorted[nextIndex - 1]
        val to = sorted[nextIndex]
        return from.point.lerp(to.point, (progress - from.progress) / (to.progress - from.progress))
    }
}
