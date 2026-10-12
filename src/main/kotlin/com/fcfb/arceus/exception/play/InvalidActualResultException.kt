package com.fcfb.arceus.exception.play

class InvalidActualResultException : Exception("Invalid actual result") {
    override fun toString(): String = "InvalidActualResultException: ${super.message}"
}
