package com.fcfb.arceus.util.jpa

import com.fcfb.arceus.enums.game.GameMode
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class GameModeConverter : AttributeConverter<GameMode, String> {
    override fun convertToDatabaseColumn(attribute: GameMode?): String? = attribute?.description

    override fun convertToEntityAttribute(dbData: String?): GameMode? = dbData?.let { GameMode.fromDescription(it) }
}
