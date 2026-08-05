package com.fcfb.arceus.scheduler

import com.fcfb.arceus.service.auth.SessionService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class CleanupExpiredTokens(
    private val sessionService: SessionService,
) {
    @Scheduled(fixedRate = 3600000)
    fun cleanUpExpiredTokens() {
        sessionService.clearExpiredTokens()
    }
}
