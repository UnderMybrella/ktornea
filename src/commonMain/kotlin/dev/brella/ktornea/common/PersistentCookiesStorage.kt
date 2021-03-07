package dev.brella.ktornea.common

import dev.brella.kornea.errors.common.map
import dev.brella.kornea.io.common.DataPool
import dev.brella.kornea.io.common.flow.readBytes
import dev.brella.kornea.toolkit.common.PrintFlow
import dev.brella.kornea.toolkit.common.SuspendInit0
import dev.brella.kornea.toolkit.common.useAndMap
import io.ktor.client.features.cookies.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.min

class PersistentCookiesStorage(private val pool: DataPool<*, *>) : CookiesStorage, SuspendInit0 {
    private val container: MutableList<Cookie> = mutableListOf()
    private val oldestCookie = atomic(0L)
    private val mutex = Mutex()

    @InternalAPI
    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val date = GMTDate()
        if (date.timestamp >= oldestCookie.value) cleanup(date.timestamp)

        return@withLock container.filter { it.matches(requestUrl) }
    }

    @KtorExperimentalAPI
    @ExperimentalUnsignedTypes
    @InternalAPI
    override suspend fun addCookie(requestUrl: Url, cookie: Cookie): Unit = mutex.withLock {
        with(cookie) {
            if (name.isBlank()) return@withLock
        }

        container.removeAll { it.name == cookie.name && it.matches(requestUrl) }
        container.add(cookie.fillDefaults(requestUrl))
        cookie.expires?.timestamp?.let { expires ->
            if (oldestCookie.value > expires) {
                oldestCookie.value = expires
            }
        }

        pool.openOutputFlow().useAndMap { flow ->
            container.forEach { cookie ->
                flow.write(renderCookieHeader(cookie).encodeToByteArray())
                flow.write('\n'.toInt())
            }
        }
    }

    private fun cleanup(timestamp: Long) {
        container.removeAll { cookie ->
            val expires = cookie.expires?.timestamp ?: return@removeAll false
            expires < timestamp
        }

        val newOldest = container.fold(Long.MAX_VALUE) { acc, cookie ->
            cookie.expires?.timestamp?.let { min(acc, it) } ?: acc
        }

        oldestCookie.value = newOldest
    }

    override fun close() {}

    @KtorExperimentalAPI
    override suspend fun init() {
        pool.openInputFlow().useAndMap { flow ->
            flow.readBytes().decodeToString().split('\n').forEach { if (it.isNotBlank()) container.add(parseServerSetCookieHeader(it)) }
        }
    }
}