package com.fcfb.arceus.repositories.appearance

import com.fcfb.arceus.model.appearance.CustomFont
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CustomFontRepository : CrudRepository<CustomFont, String>
