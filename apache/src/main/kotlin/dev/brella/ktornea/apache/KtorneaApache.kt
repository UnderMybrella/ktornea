package dev.brella.ktornea.apache

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.apache.*

/**
 * [HttpClientEngineFactory] using `org.apache.httpcomponents.httpasyncclient`
 * with the the associated configuration [ApacheEngineConfig].
 *
 * Supports HTTP/2 and HTTP/1.x requests.
 */
public object KtorneaApache : HttpClientEngineFactory<ApacheEngineConfig> {
    override fun create(block: ApacheEngineConfig.() -> Unit): HttpClientEngine {
        val config = ApacheEngineConfig().apply(block)
        return KtorneaApacheEngine(config)
    }
}

@Suppress("KDocMissingDocumentation")
public class KtorneaApacheEngineContainer : HttpClientEngineContainer {
    override val factory: HttpClientEngineFactory<*> = Apache

    override fun toString(): String = "Apache"
}
