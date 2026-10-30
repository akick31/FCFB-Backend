package com.fcfb.arceus.util

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

fun <T : Any> KClass<T>.copyProperties(
    source: T,
    target: T,
    except: Set<String> = emptySet(),
) {
    memberProperties
        .filterIsInstance<KMutableProperty1<T, Any?>>()
        .filter { it.name !in except }
        .forEach { it.set(target, it.get(source)) }
}
