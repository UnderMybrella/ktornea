package dev.brella.ktornea.common

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.features.*
import io.ktor.client.features.HttpTimeout.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.concurrent.*
import io.ktor.utils.io.core.internal.*
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
private val ALLOWED_FOR_REDIRECT: Set<HttpMethod> = setOf(HttpMethod.Get, HttpMethod.Head)

/**
 * [HttpClient] feature that handles http redirect
 */
public class GranularHttpRedirect(val enabled: Boolean?, val checkHttpMethod: Boolean?, val allowHttpsDowngrade: Boolean?) {
    @OptIn(DangerousInternalIoApi::class)
    public class Config {
        companion object : BasicAttributeKeyProvider<Config>("GranularHttpRedirectConfig")

        var enabled: Boolean? by shared(null)

        /**
         * Check if the HTTP method is allowed for redirect.
         * Only [HttpMethod.Get] and [HttpMethod.Head] is allowed for implicit redirect.
         *
         * Please note: changing this flag could lead to security issues, consider changing the request URL instead.
         */
        var checkHttpMethod: Boolean? by shared(null)

        /**
         * `true` value allows client redirect with downgrade from https to plain http.
         */
        var allowHttpsDowngrade: Boolean? by shared(null)

        constructor(enabled: Boolean? = null, checkHttpMethod: Boolean? = null, allowHttpsDowngrade: Boolean? = null)

        inline fun build() = GranularHttpRedirect(enabled, checkHttpMethod, allowHttpsDowngrade)
    }

    public companion object Feature : HttpClientFeature<Config, GranularHttpRedirect>, HttpClientEngineCapability<Config> {
        public const val DEFAULT_CHECK_HTTP_METHOD = true
        public const val DEFAULT_ALLOW_HTTPS_DOWNGRADE = false

        override val key: AttributeKey<GranularHttpRedirect> = AttributeKey("GranularHttpRedirect")

        override fun prepare(block: Config.() -> Unit): GranularHttpRedirect = Config().apply(block).build()

        @InternalAPI
        override fun install(feature: GranularHttpRedirect, scope: HttpClient) {
            scope[HttpSend].intercept { origin, context ->
                val capability = context.attributes.getOrNull(Config)
                if (capability?.enabled == false) return@intercept origin

                if ((capability?.checkHttpMethod ?: feature.checkHttpMethod ?: DEFAULT_CHECK_HTTP_METHOD) && origin.request.method !in ALLOWED_FOR_REDIRECT) {
                    return@intercept origin
                }

                handleCall(context, origin, (capability?.allowHttpsDowngrade ?: feature.allowHttpsDowngrade ?: DEFAULT_ALLOW_HTTPS_DOWNGRADE))
            }
        }

        @KtorExperimentalAPI
        @InternalAPI
        private suspend fun Sender.handleCall(
            context: HttpRequestBuilder,
            origin: HttpClientCall,
            allowHttpsDowngrade: Boolean
        ): HttpClientCall {
            if (!origin.response.status.isRedirect()) return origin

            var call = origin
            var requestBuilder = context
            val originProtocol = origin.request.url.protocol
            val originAuthority = origin.request.url.authority
            while (true) {
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

inline var Attributes.granularHttpRedirect
    get() = getOrNull(GranularHttpRedirect.Config)
    set(value) = value?.let { put(GranularHttpRedirect.Config, it) } ?: remove(GranularHttpRedirect.Config)

inline fun Attributes.granularHttpRedirect(configure: GranularHttpRedirect.Config.() -> Unit) {
    getOrNull(GranularHttpRedirect.Config, configure) ?: put(GranularHttpRedirect.Config, GranularHttpRedirect.Config().apply(configure))
}

inline fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installGranularHttp(noinline block: GranularHttpRedirect.Config.() -> Unit = {}) {
    followRedirects = false

    install(GranularHttpRedirect, block)
}

inline fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installGranularHttp(checkHttpMethod: Boolean? = null, allowHttpsDowngrade: Boolean?) {
    followRedirects = false

    install(GranularHttpRedirect) {
        checkHttpMethod?.let { this.checkHttpMethod = it }
        allowHttpsDowngrade?.let { this.allowHttpsDowngrade = it }
    }
}

inline fun HttpRequestBuilder.httpRedirect(enabled: Boolean? = null, checkHttpMethod: Boolean? = null, allowHttpsDowngrade: Boolean? = null) =
    attributes.granularHttpRedirect {
        enabled?.let { this.enabled = it }
        checkHttpMethod?.let { this.checkHttpMethod = it }
        allowHttpsDowngrade?.let { this.allowHttpsDowngrade = it }
    }

inline fun HttpRequestBuilder.httpRedirect(block: GranularHttpRedirect.Config.() -> Unit) =
    attributes.granularHttpRedirect(block)

@KtorExperimentalAPI
@InternalAPI
suspend fun HttpClient.handleRedirect(
    allowHttpsDowngrade: Boolean? = null,
    contextBuilder: HttpRequestBuilder.() -> Unit
): HttpClientCall {
    val context = HttpRequestBuilder().apply(contextBuilder)

    @Suppress("DEPRECATION_ERROR")
    val origin = execute(context)

    if (!origin.response.status.isRedirect()) return origin

    val allowHttpsDowngrade = allowHttpsDowngrade
                              ?: context.attributes.getOrNull(GranularHttpRedirect.Config)?.allowHttpsDowngrade
                              ?: feature(GranularHttpRedirect)?.allowHttpsDowngrade
                              ?: false

    var call = origin
    var requestBuilder = context
    val originProtocol = origin.request.url.protocol
    val originAuthority = origin.request.url.authority

    while (true) {
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

        @Suppress("DEPRECATION_ERROR")
        call = execute(requestBuilder)
        if (!call.response.status.isRedirect()) return call
    }
}