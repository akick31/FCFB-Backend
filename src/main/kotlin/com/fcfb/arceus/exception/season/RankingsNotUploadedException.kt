package com.fcfb.arceus.exception.season

class RankingsNotUploadedException(season: Int, week: Int) : Exception(
    "Rankings have not been uploaded for Season $season, Week $week. " +
        "Upload the Coaches Poll rankings before starting the game week.",
) {
    override fun toString(): String = "RankingsNotUploadedException: ${super.message}"
}
