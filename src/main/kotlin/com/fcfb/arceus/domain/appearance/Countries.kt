package com.fcfb.arceus.domain.appearance

object Countries {
    val ALL: List<String> =
        requireNotNull(Countries::class.java.getResource("/countries.txt"))
            .readText()
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private val known = ALL.toSet()

    fun isKnown(country: String) = country in known
}
