package com.fcfb.arceus.util

class RankingsNotUploadedException(season: Int, week: Int) : RuntimeException(
    "Rankings have not been uploaded for Season $season, Week $week. " +
        "Upload the Coaches Poll rankings before starting the game week.",
)
