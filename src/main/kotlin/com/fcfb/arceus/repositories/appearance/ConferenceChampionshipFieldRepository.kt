package com.fcfb.arceus.repositories.appearance

import com.fcfb.arceus.model.appearance.ConferenceChampionshipField
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ConferenceChampionshipFieldRepository : CrudRepository<ConferenceChampionshipField, String>
