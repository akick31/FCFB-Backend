package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.model.Offseason
import com.fcfb.arceus.repositories.OffseasonRepository
import org.springframework.stereotype.Service

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
}
