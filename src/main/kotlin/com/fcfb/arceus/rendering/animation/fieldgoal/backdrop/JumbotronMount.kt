package com.fcfb.arceus.rendering.animation.fieldgoal.backdrop

data class JumbotronMount(
    val groundY: Int,
    val restingPost: Int,
    val canLift: Boolean,
    val keepInFrame: Boolean,
    val spanWidth: Int? = null,
    val footY: Int = groundY,
    val faceBottom: Int? = null,
    val fixedScale: Boolean = false,
)
