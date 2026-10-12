package com.fcfb.arceus.exception.team

class NoCoachesFoundException : Exception("No coaches found") {
    override fun toString(): String = "NoCoachesFoundException: ${super.message}"
}
