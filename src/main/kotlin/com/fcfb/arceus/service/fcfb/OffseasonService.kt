package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.model.Offseason
import com.fcfb.arceus.repositories.OffseasonRepository
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class OffseasonService(
    private val offseasonRepository: OffseasonRepository,
) {
    fun getCurrentOffseason(): Offseason? = offseasonRepository.getCurrentOffseason()

    fun startOffseason(startDate: String) {
        offseasonRepository.save(Offseason(startDate = startDate, endDate = null))
    }

    fun endOffseason(endDate: String) {
        val currentOffseason = offseasonRepository.getCurrentOffseason() ?: return
        currentOffseason.endDate = endDate
        offseasonRepository.save(currentOffseason)
    }

    fun startOffseasonNow(): Offseason = offseasonRepository.save(Offseason(startDate = nowFormatted(), endDate = null))

    fun endOffseasonNow(): Offseason? {
        val currentOffseason = offseasonRepository.getCurrentOffseason() ?: return null
        currentOffseason.endDate = nowFormatted()
        return offseasonRepository.save(currentOffseason)
    }

    private fun nowFormatted(): String =
        ZonedDateTime.now(ZoneId.of("America/New_York")).format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
}
