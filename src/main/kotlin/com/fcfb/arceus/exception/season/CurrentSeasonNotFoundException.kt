package com.fcfb.arceus.exception.season

class CurrentSeasonNotFoundException : Exception("Current season not found") {
    override fun toString(): String = "CurrentSeasonNotFoundException: ${super.message}"
}
