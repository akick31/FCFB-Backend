package com.fcfb.arceus.service.fcfb.animation.choreography

data class OffensiveAlignment(
    val spots: List<Spot>,
    val quarterback: Int,
    val backs: List<Int>,
    val receivers: List<Int>,
    val underCenter: Boolean,
)
