package com.fcfb.arceus.enums.ranking

enum class RankingMetricType(val description: String, val implemented: Boolean, val higherIsBetter: Boolean) {
    EQUIVALENT_WINS("Pythagorean EQW", true, true),
    COLLEY_MATRIX("Colley Matrix", true, true),
    ASR("ASR", true, true),
    MARGIN_OF_VICTORY("Margin of Victory", true, true),
    SCORING_OFFENSE("Scoring Offense", true, true),
    SCORING_DEFENSE("Scoring Defense", true, false),
    POWER_RATING("Nutter Power Rating", true, true),
    COMPOSITE("Composite", true, true),
    ;

    companion object {
        fun fromString(value: String): RankingMetricType? =
            values().find { it.name.equals(value, ignoreCase = true) || it.description.equals(value, ignoreCase = true) }
    }
}
