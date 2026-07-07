package com.fcfb.arceus.service.log

import com.fcfb.arceus.model.RequestMessageLog
import com.fcfb.arceus.repositories.RequestMessageLogRepository
import org.springframework.stereotype.Component
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class RequestMessageLogService(
    private val requestMessageLogRepository: RequestMessageLogRepository,
) {
    fun logRequestMessage(requestMessageLog: RequestMessageLog): RequestMessageLog {
        if (requestMessageLog.playId == 0) {
            requestMessageLog.playId = null
        }
        requestMessageLog.messageTs =
            ZonedDateTime.now(
                ZoneId.of("America/New_York"),
            ).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
        return requestMessageLogRepository.save(requestMessageLog)
    }
}
