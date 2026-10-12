package com.fcfb.arceus.exception.ranking

class MetricNotImplementedException(metricType: String) : Exception("Metric type not yet implemented: $metricType") {
    override fun toString(): String = "MetricNotImplementedException: ${super.message}"
}
