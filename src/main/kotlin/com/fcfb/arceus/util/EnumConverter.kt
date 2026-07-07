package com.fcfb.arceus.util

import com.fcfb.arceus.enums.game.GameMode
import com.fcfb.arceus.enums.gameflow.CoinTossCall
import com.fcfb.arceus.enums.play.Scenario
import com.fcfb.arceus.enums.team.DefensivePlaybook
import com.fcfb.arceus.enums.team.OffensivePlaybook
import com.fcfb.arceus.enums.team.Subdivision
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class OffensivePlaybookConverter : AttributeConverter<OffensivePlaybook, String> {
    override fun convertToDatabaseColumn(attribute: OffensivePlaybook?): String? {
        return attribute?.description
    }

    override fun convertToEntityAttribute(dbData: String?): OffensivePlaybook? {
        return dbData?.let { OffensivePlaybook.fromString(it) }
    }
}

@Converter(autoApply = true)
class DefensivePlaybookConverter : AttributeConverter<DefensivePlaybook, String> {
    override fun convertToDatabaseColumn(attribute: DefensivePlaybook?): String? {
        return attribute?.description
    }

    override fun convertToEntityAttribute(dbData: String?): DefensivePlaybook? {
        return dbData?.let {
            try {
                DefensivePlaybook.fromString(it)
            } catch (e: IllegalArgumentException) {
                Logger.error("Error converting DefensivePlaybook: $it", e)
                null
            }
        }
    }
}

@Converter(autoApply = true)
class SubdivisionConverter : AttributeConverter<Subdivision, String> {
    override fun convertToDatabaseColumn(attribute: Subdivision?): String? {
        return attribute?.description
    }

    override fun convertToEntityAttribute(dbData: String?): Subdivision? {
        return dbData?.let { Subdivision.fromString(it) }
    }
}

@Converter(autoApply = true)
class ResultConverter : AttributeConverter<Scenario, String> {
    override fun convertToDatabaseColumn(attribute: Scenario?): String? {
        return attribute?.description
    }

    override fun convertToEntityAttribute(dbData: String?): Scenario? {
        return dbData?.let { Scenario.fromString(it) }
    }
}

@Converter(autoApply = true)
class CoinTossCallConverter : AttributeConverter<CoinTossCall, String> {
    override fun convertToDatabaseColumn(attribute: CoinTossCall?): String? {
        return attribute?.description
    }

    override fun convertToEntityAttribute(dbData: String?): CoinTossCall? {
        return dbData?.let { CoinTossCall.fromString(it) }
    }
}

@Converter(autoApply = true)
class GameModeConverter : AttributeConverter<GameMode, String> {
    override fun convertToDatabaseColumn(attribute: GameMode?): String? {
        return attribute?.description
    }

    override fun convertToEntityAttribute(dbData: String?): GameMode? {
        return dbData?.let { GameMode.fromDescription(it) }
    }
}
