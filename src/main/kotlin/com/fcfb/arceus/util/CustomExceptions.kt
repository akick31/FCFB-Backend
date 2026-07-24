package com.fcfb.arceus.util

class UserUnauthorizedException : Exception("User is unauthorized") {
    override fun toString(): String {
        return "UserUnauthorizedException: ${super.message}"
    }
}

class DiscordUserNotFoundException : Exception("Discord user not found") {
    override fun toString(): String {
        return "DiscordUserNotFoundException: ${super.message}"
    }
}

class UnableToCreateGameThreadException : Exception("Unable to create game thread in Discord") {
    override fun toString(): String {
        return "UnableToCreateGameThreadException: ${super.message}"
    }
}

class UnableToDeleteGameException : Exception("Unable to delete game") {
    override fun toString(): String {
        return "UnableToDeleteGameException: ${super.message}"
    }
}

class NoGameFoundException : Exception("No games found to start week") {
    override fun toString(): String {
        return "NoGameFoundException: ${super.message}"
    }
}

class NumberNotFoundException : Exception("Number not found") {
    override fun toString(): String {
        return "NumberNotFoundException: ${super.message}"
    }
}

class ResultNotFoundException : Exception("Result not found") {
    override fun toString(): String {
        return "ResultNotFoundException: ${super.message}"
    }
}

class InvalidScenarioException : Exception("Invalid outcome scenario") {
    override fun toString(): String {
        return "InvalidScenarioException: ${super.message}"
    }
}

class InvalidActualResultException : Exception("Invalid actual result") {
    override fun toString(): String {
        return "InvalidActualResultException: ${super.message}"
    }
}

class InvalidPlayTypeException : Exception("Invalid play type") {
    override fun toString(): String {
        return "InvalidPlayTypeException: ${super.message}"
    }
}

class InvalidHalfTimePossessionChangeException : Exception("Invalid half time possession change") {
    override fun toString(): String {
        return "InvalidHalfTimePossessionChangeException: ${super.message}"
    }
}

class InvalidResultDescriptionException : Exception("Invalid result description, could not parse result yardage") {
    override fun toString(): String {
        return "InvalidInvalidResultDescriptionException: ${super.message}"
    }
}

class CurrentSeasonNotFoundException : Exception("Current season not found") {
    override fun toString(): String {
        return "CurrentSeasonNotFoundException: ${super.message}"
    }
}

class CurrentWeekNotFoundException : Exception("Current week not found") {
    override fun toString(): String {
        return "CurrentWeekNotFoundException: ${super.message}"
    }
}

class NoCoachesFoundException : Exception("No coaches found") {
    override fun toString(): String {
        return "NoCoachesFoundException: ${super.message}"
    }
}

class NoCoachDiscordIdsFoundException : Exception("No coach Discord IDs found") {
    override fun toString(): String {
        return "NoCoachDiscordIdFoundException: ${super.message}"
    }
}

class TooManyCoachesException : Exception("Attempting to add too many coaches to a team") {
    override fun toString(): String {
        return "TooManyCoachesException: ${super.message}"
    }
}

class DefensiveNumberNotFound : Exception("Defensive number not found") {
    override fun toString(): String {
        return "DefensiveNumberNotFound: ${super.message}"
    }
}

class TeamNotFoundException(message: String) : Exception(message) {
    override fun toString(): String {
        return "TeamNotFoundException: ${super.message}"
    }
}

class GameNotFoundException(message: String) : RuntimeException(message) {
    override fun toString(): String {
        return "GameNotFoundException: ${super.message}"
    }
}

class GameStatsNotFoundException(message: String) : Exception(message) {
    override fun toString(): String {
        return "GameStatsNotFoundException: ${super.message}"
    }
}

class PlayNotFoundException(message: String) : Exception(message) {
    override fun toString(): String {
        return "PlayNotFoundException: ${super.message}"
    }
}

class ScheduleNotFoundException(message: String) : Exception(message) {
    override fun toString(): String {
        return "ScheduleNotFoundException: ${super.message}"
    }
}

class UserNotFoundException(message: String) : Exception(message) {
    override fun toString(): String {
        return "UserNotFoundException: ${super.message}"
    }
}

class InvalidCoinTossChoiceException(message: String) : Exception(message) {
    override fun toString(): String {
        return "InvalidCoinTossChoiceException: ${super.message}"
    }
}

class InvalidRankingsException(message: String) : Exception(message) {
    override fun toString(): String {
        return "InvalidRankingsException: ${super.message}"
    }
}

class RankingsNotUploadedException(season: Int, week: Int) : Exception(
    "Rankings have not been uploaded for Season $season, Week $week. " +
        "Upload the Coaches Poll rankings before starting the game week.",
) {
    override fun toString(): String {
        return "RankingsNotUploadedException: ${super.message}"
    }
}
