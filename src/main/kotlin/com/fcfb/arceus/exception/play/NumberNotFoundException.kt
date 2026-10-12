package com.fcfb.arceus.exception.play

class NumberNotFoundException : Exception("Number not found") {
    override fun toString(): String = "NumberNotFoundException: ${super.message}"
}
