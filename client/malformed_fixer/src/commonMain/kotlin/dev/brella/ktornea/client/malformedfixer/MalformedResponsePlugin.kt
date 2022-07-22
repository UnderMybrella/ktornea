package dev.brella.ktornea.client.malformedfixer

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*

public typealias MalformedResponseFilter = PipelineContext<HttpResponse, Unit>.(HttpResponse) -> MalformedResponsePlugin.Plugin.MalformBoolean
public typealias MalformedResponseTransformResponse = PipelineContext<HttpResponse, Unit>.(HttpResponse) -> HttpResponse?
public typealias MalformedResponseTransformResponseBody = PipelineContext<HttpResponse, Unit>.(HttpResponse) -> Any?

public class MalformedResponsePlugin private constructor(
    private val filter: MalformedResponseFilter?,
    private val transformResponse: MalformedResponseTransformResponse?,
    private val transformResponseBody: MalformedResponseTransformResponseBody?,
) {
    @KtorDsl
    public class Config {
        public var filterRequests: MalformedResponseFilter? = null
        public var transformResponse: MalformedResponseTransformResponse? = null
        public var transformResponseBody: MalformedResponseTransformResponseBody? = null
    }

    public companion object Plugin : HttpClientPlugin<Config, MalformedResponsePlugin> {

        override val key: AttributeKey<MalformedResponsePlugin> = AttributeKey("MalformedRedirect")

        override fun prepare(block: Config.() -> Unit): MalformedResponsePlugin {
            val config = Config()
            config.block()
            return MalformedResponsePlugin(
                config.filterRequests,
                config.transformResponse,
                config.transformResponseBody
            )
        }

        @OptIn(InternalAPI::class)
        override fun install(plugin: MalformedResponsePlugin, scope: HttpClient) {
            scope.receivePipeline.intercept(HttpReceivePipeline.Before) { response ->
                var malformed = MalformBoolean.YES
                plugin.filter?.let { filter -> malformed = filter(response) }

                if (malformed == MalformBoolean.NO) {
                    proceed()
                    return@intercept
                }

                val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1
                val transferEncoding = response.headers["Transfer-Encoding"]
                val connection = response.headers["Connection"]

                if (malformed == MalformBoolean.YES && transferEncoding != null) {
                    when (isTransferEncodingChunked(transferEncoding)) {
                        MalformBoolean.YES -> {
                            proceed()
                            return@intercept
                        }
                        MalformBoolean.MALFORMED -> malformed = MalformBoolean.MALFORMED
                        else -> {}
                    }
                }

                if (malformed == MalformBoolean.YES && contentLength != -1L) {
                    proceed()
                    return@intercept
                }

                if (malformed == MalformBoolean.YES && connection?.equals("close", true) == true) {
                    proceed()
                    return@intercept
                }

                proceedWith(
                    plugin.transformResponse?.invoke(this, response)
                        ?: DefaultHttpResponse(
                            response.call,
                            HttpResponseData(
                                response.status,
                                response.requestTime,
                                response.headers,
                                response.version,
                                plugin.transformResponseBody?.invoke(this, response) ?: ByteReadChannel.Empty,
                                response.coroutineContext
                            )
                        )
                )
            }
        }

        public enum class MalformBoolean {
            YES,
            NO,
            MALFORMED
        }

        private fun isTransferEncodingChunked(transferEncoding: String): MalformBoolean {
            if (transferEncoding.equals("chunked", true)) {
                return MalformBoolean.YES
            }
            if (transferEncoding.equals("identity", true)) {
                return MalformBoolean.NO
            }

            var chunked = MalformBoolean.NO
            transferEncoding.split(",").forEach {
                when (val name = it.trim().lowercase()) {
                    "chunked" -> {
                        if (chunked == MalformBoolean.YES) return MalformBoolean.MALFORMED
                        chunked = MalformBoolean.YES
                    }
                    "identity" -> {
                        // ignore this token
                    }
                    else -> return MalformBoolean.MALFORMED
                }
            }

            return chunked
        }
    }
}