package com.fcfb.arceus.exception.ranking

class InvalidRankingMetricException(message: String) : Exception(message) {
    override fun toString(): String = "InvalidRankingMetricException: ${super.message}"
}
