package com.fcfb.arceus.util.jpa

import com.fcfb.arceus.enums.gameflow.CoinTossCall
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class CoinTossCallConverter : AttributeConverter<CoinTossCall, String> {
    override fun convertToDatabaseColumn(attribute: CoinTossCall?): String? = attribute?.description

    override fun convertToEntityAttribute(dbData: String?): CoinTossCall? = dbData?.let { CoinTossCall.fromString(it) }
}
