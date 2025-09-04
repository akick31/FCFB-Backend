package com.fcfb.arceus.enums.records

/**
 * Enum representing the type of record
 */
enum class RecordType {
    /**
     * Record for a single game performance (highest value)
     * Examples: Most yards in a game, most touchdowns in a game
     */
    SINGLE_GAME,
    
    /**
     * Record for a single game performance (lowest value)
     * Examples: Fewest yards in a game, fewest touchdowns in a game
     */
    SINGLE_GAME_LOWEST,
    
    /**
     * Record for single season total/average performance (highest value)
     * Examples: Most total yards in a single season EVER, highest average diff in a single season EVER
     */
    SINGLE_SEASON,
    
    /**
     * Record for single season total/average performance (lowest value)
     * Examples: Fewest total yards in a single season EVER, lowest average diff in a single season EVER
     */
    SINGLE_SEASON_LOWEST,
    
    /**
     * General record that doesn't need season/game distinction (highest value)
     * Examples: Longest field goal ever, fastest response time ever
     */
    GENERAL,
    
    /**
     * General record that doesn't need season/game distinction (lowest value)
     * Examples: Shortest field goal ever, slowest response time ever
     */
    GENERAL_LOWEST
}
