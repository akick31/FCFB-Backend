package com.fcfb.arceus.repositories

import com.fcfb.arceus.model.CustomFont
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CustomFontRepository : CrudRepository<CustomFont, String>
