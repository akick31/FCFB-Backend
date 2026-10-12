package com.fcfb.arceus.util

import com.fcfb.arceus.enums.team.OffensivePlaybook
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class OffensivePlaybookConverter : AttributeConverter<OffensivePlaybook, String> {
    override fun convertToDatabaseColumn(attribute: OffensivePlaybook?): String? = attribute?.description

    override fun convertToEntityAttribute(dbData: String?): OffensivePlaybook? = dbData?.let { OffensivePlaybook.fromString(it) }
}
