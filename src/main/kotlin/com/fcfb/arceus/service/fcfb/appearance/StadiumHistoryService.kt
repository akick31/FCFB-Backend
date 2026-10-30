package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.dto.request.StadiumDesignRequest
import com.fcfb.arceus.dto.request.StadiumUpdateRequest
import com.fcfb.arceus.dto.response.StadiumChangeResponse
import com.fcfb.arceus.dto.response.StadiumResponse
import com.fcfb.arceus.enums.animation.StadiumChangeAction
import com.fcfb.arceus.exception.team.InvalidUniformException
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.snapshotJson
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StadiumHistoryService(
    private val stadiumChangeLogService: StadiumChangeLogService,
    private val stadiumService: StadiumService,
) {
    fun history(venue: String): List<StadiumChangeResponse> = stadiumChangeLogService.history(venue)

    @Transactional
    fun revert(changeId: Long): StadiumResponse {
        val entry = stadiumChangeLogService.change(changeId)
        val before = stadiumChangeLogService.snapshotOf(entry) ?: throw InvalidUniformException("There is no earlier version to go back to")
        val restoresLocation = AuthContext().isAdminOrCommissioner()
        val request =
            StadiumUpdateRequest(
                city = before.city.takeIf { restoresLocation },
                state = before.state.takeIf { restoresLocation },
                country = before.country.takeIf { restoresLocation },
                capacity = before.capacity,
                design = snapshotJson.convertValue(before.design, StadiumDesignRequest::class.java),
            )
        return stadiumService.update(entry.venue, request, StadiumChangeAction.REVERT)
    }
}
