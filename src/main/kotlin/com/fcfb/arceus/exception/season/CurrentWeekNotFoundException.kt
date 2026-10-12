package com.fcfb.arceus.exception.season

class CurrentWeekNotFoundException : Exception("Current week not found") {
    override fun toString(): String = "CurrentWeekNotFoundException: ${super.message}"
}
