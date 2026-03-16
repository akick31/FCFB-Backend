package com.fcfb.arceus.enums.team

enum class Conference(val description: String, val logoUrl: String? = null) {
    ACC("ACC", "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/1.png"),
    AMERICAN("American", "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/151.png"),
    BIG_12("Big 12", "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/4.png"),
    BIG_TEN("Big Ten", "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/5.png"),
    FAKE_TEAM("Fake Team"),
    FBS_INDEPENDENT("FBS Independent", "https://logos-world.net/wp-content/uploads/2025/01/Division-I-FBS-Independents-Logo-500x281.png"),
    MAC("MAC", "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/15.png"),
    MOUNTAIN_WEST("Mountain West", "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/17.png"),
    PAC_12("Pac-12", "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/9.png"),
    SEC("SEC", "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/8.png"),
    SUN_BELT("Sun Belt", "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/37.png"),
    MISSOURI_VALLEY("Missouri Valley", "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/21.png"),
    COLONIAL("Colonial", "https://images.seeklogo.com/logo-png/49/2/colonial-athletic-association-logo-png_seeklogo-490062.png"),
    NEC("NEC", "https://a.espncdn.com/i/teamlogos/ncaa_conf/500/25.png"),
    ;

    companion object {
        fun fromString(description: String): Conference? {
            return Conference.values().find { it.description == description }
        }
    }
}
