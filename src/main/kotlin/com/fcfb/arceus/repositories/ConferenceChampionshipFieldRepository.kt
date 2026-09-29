package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.ConferenceChampionshipField
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ConferenceChampionshipFieldRepository : CrudRepository<ConferenceChampionshipField, String>
