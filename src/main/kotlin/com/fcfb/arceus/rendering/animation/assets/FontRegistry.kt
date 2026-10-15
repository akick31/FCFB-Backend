package com.fcfb.arceus.rendering.animation.assets

import java.util.concurrent.ConcurrentHashMap

object FontRegistry {
    private val families = ConcurrentHashMap<String, String>()

    fun register(
        name: String,
        family: String,
    ) {
        families[name.uppercase()] = family
    }

    fun unregister(name: String) {
        families.remove(name.uppercase())
    }

    fun familyFor(name: String?): String? = name?.let { families[it.uppercase()] }
}
