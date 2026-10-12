package com.fcfb.arceus.model.animation.choreography

data class DefensiveAlignment(
    val spots: List<Spot>,
    val linemen: List<Int>,
    val linebackers: List<Int>,
    val secondary: List<Int>,
)
