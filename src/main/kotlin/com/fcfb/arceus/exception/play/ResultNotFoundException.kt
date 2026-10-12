package com.fcfb.arceus.exception.play

class ResultNotFoundException : Exception("Result not found") {
    override fun toString(): String = "ResultNotFoundException: ${super.message}"
}
