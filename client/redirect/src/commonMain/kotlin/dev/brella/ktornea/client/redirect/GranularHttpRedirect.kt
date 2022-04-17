package dev.brella.ktornea.client.redirect

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.events.*
import io.ktor.http.*
import io.ktor.util.*

private val ALLOWED_FOR_REDIRECT: Set<HttpMethod> = setOf(HttpMethod.Get, HttpMethod.Head)

/**
 * An [HttpClient] plugin that handles HTTP redirects
 */
public class GranularHttpRedirect private constructor(
    private val enabledByDefault: Boolean,
    private val checkHttpMethod: Boolean,
    private val allowHttpsDowngrade: Boolean
) {

    @KtorDsl
    public class Config {

        /**
         * Enable HTTP redirects by default.
         */
        public var enabled: Boolean = true

        /**
         * Checks whether the HTTP method is allowed for the redirect.
         * Only [HttpMethod.Get] and [HttpMethod.Head] are allowed for implicit redirection.
         *
         * Please note: changing this flag could lead to security issues, consider changing the request URL instead.
         */
        public var checkHttpMethod: Boolean = true

        /**
         * `true` allows a client to make a redirect with downgrading from HTTPS to plain HTTP.
         */
        public var allowHttpsDowngrade: Boolean = false
    }

    public companion object Plugin : HttpClientPlugin<Config, GranularHttpRedirect> {
        override val key: AttributeKey<GranularHttpRedirect> = AttributeKey("GranularHttpRedirect")
        public val configKey: AttributeKey<Config> = AttributeKey("GranularHttpRedirectConfig")

        /**
         * Occurs when receiving a response with a redirect message.
         */
        public val HttpResponseRedirect: EventDefinition<HttpResponse> = EventDefinition()

        override fun prepare(block: Config.() -> Unit): GranularHttpRedirect {
            val config = Config().apply(block)
            return GranularHttpRedirect(
                enabledByDefault = config.enabled,
                checkHttpMethod = config.checkHttpMethod,
                allowHttpsDowngrade = config.allowHttpsDowngrade
            )
        }

        override fun install(plugin: GranularHttpRedirect, scope: HttpClient) {
            scope.plugin(HttpSend).intercept { context ->
                val origin = execute(context)

                val scopedConfig = context.attributes.getOrNull(configKey)
                if (!(plugin.enabledByDefault || scopedConfig?.enabled == true)) {
                    return@intercept origin
                }

                val checkHttpMethod = scopedConfig?.checkHttpMethod ?: plugin.checkHttpMethod
                if (checkHttpMethod && origin.request.method !in ALLOWED_FOR_REDIRECT) {
                    return@intercept origin
                }

                val allowHttpsDowngrade = scopedConfig?.allowHttpsDowngrade ?: plugin.allowHttpsDowngrade
                handleCall(context, origin, allowHttpsDowngrade, scope)
            }
        }

        @OptIn(InternalAPI::class)
        private suspend fun Sender.handleCall(
            context: HttpRequestBuilder,
            origin: HttpClientCall,
            allowHttpsDowngrade: Boolean,
            client: HttpClient
        ): HttpClientCall {
            if (!origin.response.status.isRedirect()) return origin

            var call = origin
            var requestBuilder = context
            val originProtocol = origin.request.url.protocol
            val originAuthority = origin.request.url.authority

            while (true) {
                client.monitor.raise(HttpResponseRedirect, call.response)

                val location = call.response.headers[HttpHeaders.Location]

                requestBuilder = HttpRequestBuilder().apply {
                    takeFromWithExecutionContext(requestBuilder)
                    url.parameters.clear()

                    location?.let { url.takeFrom(it) }

                    /**
                     * Disallow redirect with a security downgrade.
                     */
                    if (!allowHttpsDowngrade && originProtocol.isSecure() && !url.protocol.isSecure()) {
                        return call
                    }

                    if (originAuthority != url.authority) {
                        headers.remove(HttpHeaders.Authorization)
                    }
                }

                call = execute(requestBuilder)
                if (!call.response.status.isRedirect()) return call
            }
        }
    }
}

private fun HttpStatusCode.isRedirect(): Boolean = when (value) {
    HttpStatusCode.MovedPermanently.value,
    HttpStatusCode.Found.value,
    HttpStatusCode.TemporaryRedirect.value,
    HttpStatusCode.PermanentRedirect.value,
    HttpStatusCode.SeeOther.value -> true
    else -> false
}

public inline fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installGranularHttp(noinline block: GranularHttpRedirect.Config.() -> Unit = {}) {
    followRedirects = false

    install(GranularHttpRedirect, block)
}

public inline fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installGranularHttp(
    enabledByDefault: Boolean? = null,
    checkHttpMethod: Boolean? = null,
    allowHttpsDowngrade: Boolean?
) {
    followRedirects = false

    install(GranularHttpRedirect) {
        enabledByDefault?.let { this.enabled = it }
        checkHttpMethod?.let { this.checkHttpMethod = it }
        allowHttpsDowngrade?.let { this.allowHttpsDowngrade = it }
    }
}

public inline var Attributes.granularHttpRedirect: GranularHttpRedirect.Config?
    get() = getOrNull(GranularHttpRedirect.configKey)
    set(value) = value?.let { put(GranularHttpRedirect.configKey, it) } ?: remove(GranularHttpRedirect.configKey)

public inline fun Attributes.granularHttpRedirect(configure: GranularHttpRedirect.Config.() -> Unit) {
    getOrNull(GranularHttpRedirect.configKey)?.let(configure)
        ?: put(GranularHttpRedirect.configKey, GranularHttpRedirect.Config().apply(configure))
}

public inline fun HttpRequestBuilder.httpRedirect(
    enabled: Boolean? = null,
    checkHttpMethod: Boolean? = null,
    allowHttpsDowngrade: Boolean? = null
): Unit =
    attributes.granularHttpRedirect {
        enabled?.let { this.enabled = it }
        checkHttpMethod?.let { this.checkHttpMethod = it }
        allowHttpsDowngrade?.let { this.allowHttpsDowngrade = it }
    }

public inline fun HttpRequestBuilder.httpRedirect(block: GranularHttpRedirect.Config.() -> Unit): Unit =
    attributes.granularHttpRedirect(block)

