package com.fcfb.arceus.util

import java.util.Collections

/** A thread-safe, size-bounded LRU cache; the eldest entry is evicted once the cap is exceeded. */
class BoundedCache<K : Any, V : Any>(private val maxSize: Int) {
    private val map =
        Collections.synchronizedMap(
            object : LinkedHashMap<K, V>(64, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean = size > maxSize
            },
        )

    fun get(key: K): V? = map[key]

    fun put(
        key: K,
        value: V,
    ) {
        map[key] = value
    }

    fun getOrPut(
        key: K,
        supplier: () -> V,
    ): V = get(key) ?: supplier().also { put(key, it) }
}
