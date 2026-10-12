package com.fcfb.arceus.util

import com.fcfb.arceus.enums.play.Scenario
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class ResultConverter : AttributeConverter<Scenario, String> {
    override fun convertToDatabaseColumn(attribute: Scenario?): String? = attribute?.description

    override fun convertToEntityAttribute(dbData: String?): Scenario? = dbData?.let { Scenario.fromString(it) }
}
