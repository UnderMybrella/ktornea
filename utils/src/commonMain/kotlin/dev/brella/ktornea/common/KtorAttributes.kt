package dev.brella.ktornea.common

import io.ktor.util.*

interface AttributeKeyProvider<T> {
    val key: AttributeKey<T>
}

open class BasicAttributeKeyProvider<T>(name: String): AttributeKeyProvider<T> {
    override val key: AttributeKey<T> = AttributeKey(name)
}

inline operator fun <T: Any> Attributes.get(provider: AttributeKeyProvider<T>): T =
    get(provider.key)

inline fun <T: Any> Attributes.getOrNull(provider: AttributeKeyProvider<T>): T? =
    getOrNull(provider.key)

inline fun <T: Any, R> Attributes.getOrNull(provider: AttributeKeyProvider<T>, block: T.() -> R): R? =
    getOrNull(provider.key)?.let(block)

inline fun <T: Any> Attributes.put(provider: AttributeKeyProvider<T>, value: T): Unit =
    put(provider.key, value)

inline operator fun <T: Any> Attributes.set(provider: AttributeKeyProvider<T>, value: T): Unit =
    put(provider.key, value)

inline fun <T: Any> Attributes.remove(provider: AttributeKeyProvider<T>): Unit =
    remove(provider.key)