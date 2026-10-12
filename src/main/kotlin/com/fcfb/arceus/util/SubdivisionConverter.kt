package com.fcfb.arceus.util

import com.fcfb.arceus.enums.team.Subdivision
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class SubdivisionConverter : AttributeConverter<Subdivision, String> {
    override fun convertToDatabaseColumn(attribute: Subdivision?): String? = attribute?.description

    override fun convertToEntityAttribute(dbData: String?): Subdivision? = dbData?.let { Subdivision.fromString(it) }
}
