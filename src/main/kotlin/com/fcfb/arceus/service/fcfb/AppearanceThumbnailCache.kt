package com.fcfb.arceus.service.fcfb

import com.fcfb.arceus.util.BoundedCache
import org.springframework.stereotype.Component

@Component
class AppearanceThumbnailCache {
    private val cache = BoundedCache<String, ByteArray>(MAX_ENTRIES)

    fun key(
        team: String,
        view: String,
    ): String = "${team.lowercase()}|${view.uppercase()}"

    fun getOrRender(
        key: String,
        render: () -> ByteArray,
    ): ByteArray = cache.getOrPut(key, render)

    fun evictTeam(team: String) = cache.removeMatching { it.startsWith("${team.lowercase()}|") }

    companion object {
        private const val MAX_ENTRIES = 2048
    }
}
