package com.fcfb.arceus.service.fcfb.animation

import java.util.concurrent.ConcurrentHashMap

/** Maps an uploaded font's key to the AWT family name it registered under, so [EndZoneFonts] can resolve custom fonts. */
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
