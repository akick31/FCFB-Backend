package com.fcfb.arceus.exception.play

class DefensiveNumberNotFound : Exception("Defensive number not found") {
    override fun toString(): String = "DefensiveNumberNotFound: ${super.message}"
}
