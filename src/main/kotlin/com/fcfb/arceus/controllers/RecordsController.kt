package com.fcfb.arceus.controllers

import com.fcfb.arceus.enums.records.RecordType
import com.fcfb.arceus.enums.records.Stats
import com.fcfb.arceus.model.Record
import com.fcfb.arceus.models.requests.GenerateRecordRequest
import com.fcfb.arceus.models.requests.RecordFilterRequest
import com.fcfb.arceus.service.fcfb.RecordService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiConstants.FULL_PATH}/records")
@CrossOrigin(origins = ["*"])
class RecordsController(
    private val recordService: RecordService,
) {
    /**
     * Get all records with optional filtering
     */
    @GetMapping("/all")
    fun getAllRecords(
        @RequestParam(required = false) recordName: Stats?,
        @RequestParam(required = false) recordType: RecordType?,
        @RequestParam(required = false) seasonNumber: Int?,
        @RequestParam(required = false) team: String?,
    ): ResponseEntity<List<Record>> {
        val filter = RecordFilterRequest(recordName, recordType, seasonNumber, team)
        val records = recordService.getFilteredRecords(filter)
        return ResponseEntity.ok(records)
    }

    /**
     * Get a specific record by stat name and record type
     */
    @GetMapping("")
    fun getRecord(
        @RequestParam recordName: Stats,
        @RequestParam recordType: RecordType,
    ): ResponseEntity<Record> {
        val record = recordService.getRecord(recordName, recordType)
        return if (record != null) {
            ResponseEntity.ok(record)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Generate all records (recalculate all records)
     */
    @PostMapping("/generate/all")
    fun generateAllRecords(): ResponseEntity<String> {
        return try {
            recordService.generateAllRecords()
            ResponseEntity.ok("All records generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating records: ${e.message}")
        }
    }

    /**
     * Generate a specific record (recalculate a specific record)
     */
    @PostMapping("/generate")
    fun generateRecord(
        @RequestBody request: GenerateRecordRequest,
    ): ResponseEntity<String> {
        return try {
            recordService.generateRecord(request.recordName, request.recordType)
            ResponseEntity.ok("Record ${request.recordName} (${request.recordType}) generated successfully")
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Error generating record: ${e.message}")
        }
    }

    /**
     * Get all records for a specific season
     */
    @GetMapping("/season")
    fun getRecordsBySeason(
        @RequestParam seasonNumber: Int,
    ): ResponseEntity<List<Record>> {
        val filter = RecordFilterRequest(seasonNumber = seasonNumber)
        val records = recordService.getFilteredRecords(filter)
        return ResponseEntity.ok(records)
    }

    /**
     * Get all records for a specific team
     */
    @GetMapping("/team")
    fun getRecordsByTeam(
        @RequestParam team: String,
    ): ResponseEntity<List<Record>> {
        val filter = RecordFilterRequest(team = team)
        val records = recordService.getFilteredRecords(filter)
        return ResponseEntity.ok(records)
    }

    /**
     * Get all records for a specific stat type
     */
    @GetMapping("/stat")
    fun getRecordsByStat(
        @RequestParam recordName: Stats,
    ): ResponseEntity<List<Record>> {
        val filter = RecordFilterRequest(recordName = recordName)
        val records = recordService.getFilteredRecords(filter)
        return ResponseEntity.ok(records)
    }
}
