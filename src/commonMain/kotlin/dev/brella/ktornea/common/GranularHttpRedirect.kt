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
import kotlinx.atomicfu.atomic
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
private val ALLOWED_FOR_REDIRECT: Set<HttpMethod> = setOf(HttpMethod.Get, HttpMethod.Head)

/**
 * [HttpClient] feature that handles http redirect
 */
public class GranularHttpRedirect {
    private val _checkHttpMethod = atomic(true)
    private val _allowHttpsDowngrade = atomic(false)

    /**
     * Check if the HTTP method is allowed for redirect.
     * Only [HttpMethod.Get] and [HttpMethod.Head] is allowed for implicit redirect.
     *
     * Please note: changing this flag could lead to security issues, consider changing the request URL instead.
     */
    public var checkHttpMethod: Boolean
        get() = _checkHttpMethod.value
        set(value) {
            _checkHttpMethod.value = value
        }

    /**
     * `true` value allows client redirect with downgrade from https to plain http.
     */
    public var allowHttpsDowngrade: Boolean
        get() = _allowHttpsDowngrade.value
        set(value) {
            _allowHttpsDowngrade.value = value
        }

    public data class GranularHttpRedirectCapability(val enabled: Boolean? = null, val checkHttpMethod: Boolean? = null, val allowHttpsDowngrade: Boolean? = null)

    public companion object Feature : HttpClientFeature<GranularHttpRedirect, GranularHttpRedirect>, HttpClientEngineCapability<GranularHttpRedirectCapability> {
        override val key: AttributeKey<GranularHttpRedirect> = AttributeKey("HttpRedirect")

        override fun prepare(block: GranularHttpRedirect.() -> Unit): GranularHttpRedirect = GranularHttpRedirect().apply(block)

        @InternalAPI
        override fun install(feature: GranularHttpRedirect, scope: HttpClient) {
            scope[HttpSend].intercept { origin, context ->
                val capability = context.getCapabilityOrNull(this@Feature)
                if (capability?.enabled == false) return@intercept origin

                if ((capability?.checkHttpMethod ?: feature.checkHttpMethod) && origin.request.method !in ALLOWED_FOR_REDIRECT) {
                    return@intercept origin
                }

                handleCall(context, origin, (capability?.allowHttpsDowngrade ?: feature.allowHttpsDowngrade))
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
                              ?: context.getCapabilityOrNull(GranularHttpRedirect)?.allowHttpsDowngrade
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