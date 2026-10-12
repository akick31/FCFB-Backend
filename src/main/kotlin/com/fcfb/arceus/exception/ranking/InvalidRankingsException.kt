package com.fcfb.arceus.exception.ranking

class InvalidRankingsException(message: String) : Exception(message) {
    override fun toString(): String = "InvalidRankingsException: ${super.message}"
}
