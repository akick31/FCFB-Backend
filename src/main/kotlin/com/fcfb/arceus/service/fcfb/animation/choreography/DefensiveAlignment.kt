package com.fcfb.arceus.service.fcfb.animation.choreography

data class DefensiveAlignment(
    val spots: List<Spot>,
    val linemen: List<Int>,
    val linebackers: List<Int>,
    val secondary: List<Int>,
)
