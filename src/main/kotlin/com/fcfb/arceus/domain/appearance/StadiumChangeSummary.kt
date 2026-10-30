package com.fcfb.arceus.domain.appearance

import com.fcfb.arceus.model.appearance.StadiumSnapshot
import com.fcfb.arceus.util.snapshotJson

object StadiumChangeSummary {
    private const val MAX_LENGTH = 500

    fun between(
        before: StadiumSnapshot,
        after: StadiumSnapshot,
    ): String? {
        val changes = detailChanges(before, after) + designChanges(before, after)
        if (changes.isEmpty()) return null
        val joined = changes.joinToString("; ")
        return if (joined.length <= MAX_LENGTH) joined else joined.take(MAX_LENGTH - 1) + "…"
    }

    private fun detailChanges(
        before: StadiumSnapshot,
        after: StadiumSnapshot,
    ): List<String> =
        listOf(
            Triple("Name", before.name, after.name),
            Triple("City", before.city, after.city),
            Triple("State / province", before.state, after.state),
            Triple("Country", before.country, after.country),
            Triple("Capacity", before.capacity, after.capacity),
            Triple("Active", before.active, after.active),
        ).filter { (_, old, new) -> old != new }
            .map { (label, old, new) -> "$label: ${shown(old)} → ${shown(new)}" }

    private fun designChanges(
        before: StadiumSnapshot,
        after: StadiumSnapshot,
    ): List<String> {
        val old = snapshotJson.convertValue(before.design, Map::class.java)
        val new = snapshotJson.convertValue(after.design, Map::class.java)
        return new.keys
            .map { it.toString() }
            .filter { old[it] != new[it] }
            .map { "${labelOf(it)}: ${shown(old[it])} → ${shown(new[it])}" }
    }

    private fun labelOf(key: String): String {
        val words = key.replace(Regex("([A-Z])"), " $1").lowercase().trim()
        return if (words.startsWith("right ")) "Right end, ${words.removePrefix("right ")}" else "Left end, $words"
    }

    private fun shown(value: Any?): String = value?.toString()?.takeIf { it.isNotBlank() } ?: "none"
}
