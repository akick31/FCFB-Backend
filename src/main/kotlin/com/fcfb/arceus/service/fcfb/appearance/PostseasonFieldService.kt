package com.fcfb.arceus.service.fcfb.appearance

import com.fcfb.arceus.dto.request.PostseasonFieldRequest
import org.springframework.stereotype.Service

@Service
class PostseasonFieldService(
    private val playoffFieldService: PlayoffFieldService,
    private val conferenceChampionshipFieldService: ConferenceChampionshipFieldService,
) {
    fun getAllPlayoffFields() = playoffFieldService.getAll()

    fun getPlayoffField(round: String) = playoffFieldService.getField(round)

    fun updatePlayoffField(
        round: String,
        request: PostseasonFieldRequest,
    ) = playoffFieldService.updateField(round, request)

    fun getAllConferenceChampionshipFields() = conferenceChampionshipFieldService.getAll()

    fun getConferenceChampionshipField(conference: String) = conferenceChampionshipFieldService.getField(conference)

    fun updateConferenceChampionshipField(
        conference: String,
        request: PostseasonFieldRequest,
    ) = conferenceChampionshipFieldService.updateField(conference, request)
}
