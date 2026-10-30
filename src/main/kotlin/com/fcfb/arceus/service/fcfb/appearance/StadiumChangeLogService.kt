package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.domain.appearance.StadiumChangeSummary
import com.fcfb.arceus.dto.response.StadiumChangeResponse
import com.fcfb.arceus.enums.animation.StadiumChangeAction
import com.fcfb.arceus.exception.team.InvalidUniformException
import com.fcfb.arceus.model.appearance.StadiumChangeLog
import com.fcfb.arceus.model.appearance.StadiumSnapshot
import com.fcfb.arceus.repositories.appearance.StadiumChangeLogRepository
import com.fcfb.arceus.service.fcfb.account.UserService
import com.fcfb.arceus.util.AuthContext
import com.fcfb.arceus.util.snapshotJson
import org.springframework.stereotype.Service

@Service
class StadiumChangeLogService(
    private val stadiumChangeLogRepository: StadiumChangeLogRepository,
    private val userService: UserService,
) {
    fun record(
        venue: String,
        action: StadiumChangeAction,
        before: StadiumSnapshot?,
        after: StadiumSnapshot,
        note: String? = null,
    ) {
        val diff = before?.let { StadiumChangeSummary.between(it, after) }
        if (action == StadiumChangeAction.UPDATE && diff == null) return
        val entry =
            StadiumChangeLog().apply {
                this.venue = venue
                this.action = action
                summary = listOfNotNull(note, diff).joinToString(". ").ifBlank { null }
                changedBy = currentUsername()
                beforeJson = before?.let { snapshotJson.writeValueAsString(it) }
            }
        stadiumChangeLogRepository.save(entry)
    }

    fun history(venue: String): List<StadiumChangeResponse> =
        stadiumChangeLogRepository.findAllByVenueOrderByChangedAtDescIdDesc(venue.trim()).map { entry ->
            StadiumChangeResponse(
                id = entry.id ?: 0,
                action = entry.action,
                summary = entry.summary,
                changedBy = entry.changedBy,
                changedAt = entry.changedAt,
                before = snapshotOf(entry),
            )
        }

    fun change(id: Long): StadiumChangeLog =
        stadiumChangeLogRepository.findById(id).orElseThrow { InvalidUniformException("No stadium change with id $id") }

    fun snapshotOf(entry: StadiumChangeLog): StadiumSnapshot? =
        entry.beforeJson?.let {
            snapshotJson.readValue(it, StadiumSnapshot::class.java)
        }

    private fun currentUsername(): String =
        AuthContext().currentUserId()?.let { userService.getUserById(it).username } ?: AuthContext().currentPrincipal()
}
