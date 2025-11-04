package com.fcfb.arceus.dto

import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats

/**
 * Request object for generating a specific record
 */
data class GenerateRecordRequest(
    val recordName: Stats,
    val recordType: RecordType,
)

/**
 * Request object for filtering records
 */
data class RecordFilterRequest(
    val recordName: Stats? = null,
    val recordType: RecordType? = null,
    val seasonNumber: Int? = null,
    val team: String? = null,
)
