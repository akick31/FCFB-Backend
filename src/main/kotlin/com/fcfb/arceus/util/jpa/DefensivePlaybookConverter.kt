package com.fcfb.arceus.util.jpa

import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.util.Logger
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class DefensivePlaybookConverter : AttributeConverter<DefensivePlaybook, String> {
    override fun convertToDatabaseColumn(attribute: DefensivePlaybook?): String? = attribute?.description

    override fun convertToEntityAttribute(dbData: String?): DefensivePlaybook? {
        return dbData?.let {
            try {
                DefensivePlaybook.fromString(it)
            } catch (e: IllegalArgumentException) {
                Logger().error("Error converting DefensivePlaybook: $it", e)
                null
            }
        }
    }
}
