package com.fcfb.arceus.exception.play

class InvalidPlayTypeException : Exception("Invalid play type") {
    override fun toString(): String = "InvalidPlayTypeException: ${super.message}"
}
