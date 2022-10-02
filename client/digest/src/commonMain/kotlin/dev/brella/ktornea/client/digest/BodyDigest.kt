package dev.brella.ktornea.client.digest

import io.ktor.client.*
import io.ktor.client.content.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import kotlin.coroutines.CoroutineContext

public typealias BodyDigestChunkedListener = suspend (buffer: ByteArray, offset: Int, len: Int) -> Unit
public typealias BodyDigestFinishedListener = suspend HttpRequestBuilder.() -> Unit
public typealias BodyDigestCacheConstructor = PipelineContext<Any, HttpRequestBuilder>.(OutgoingContent, CoroutineContext) -> DigestCache

public data class BodyDigestListener(
    val chunkedListener: BodyDigestChunkedListener? = null,
    val finishedListener: BodyDigestFinishedListener? = null,
)

@KtorDsl
public class BodyDigest {
    public val listeners: MutableList<BodyDigestListener> = ArrayList()
    public var cacheConstructor: BodyDigestCacheConstructor? = null

    public companion object Plugin : HttpClientPlugin<BodyDigest, BodyDigest> {
        override val key: AttributeKey<BodyDigest> = AttributeKey("BodyDigest")
        public val configKey: AttributeKey<BodyDigest> = AttributeKey("${key.name}_Config")

        override fun prepare(block: BodyDigest.() -> Unit): BodyDigest =
            BodyDigest().apply(block)

        override fun install(plugin: BodyDigest, scope: HttpClient) {
            val digestContentPhase = PipelinePhase("DigestContent")
            scope.requestPipeline.insertPhaseBefore(reference = HttpRequestPipeline.Send, phase = digestContentPhase)
            scope.requestPipeline.intercept(digestContentPhase) { content ->
                val localConfig = context.attributes
                    .getOrNull(configKey)

                if (plugin.listeners.isEmpty() && localConfig?.listeners.isNullOrEmpty()) return@intercept

                val digestContent = (localConfig?.cacheConstructor ?: plugin.cacheConstructor)
                    ?.invoke(this, content as OutgoingContent, context.executionContext)
                    ?: MemoryDigestCache(content as OutgoingContent, context.executionContext)

                plugin.listeners.forEach { listeners ->
                    listeners.chunkedListener?.let { digestContent.digest(it) }
                    listeners.finishedListener?.invoke(context)
                }

                localConfig?.listeners?.forEach { listeners ->
                    listeners.chunkedListener?.let { digestContent.digest(it) }
                    listeners.finishedListener?.invoke(context)
                }

                proceedWith(digestContent)
            }
        }
    }
}

public fun <T : HttpClientEngineConfig> HttpClientConfig<T>.BodyDigest(block: BodyDigest.() -> Unit = {}): Unit =
    install(BodyDigest, block)

public inline var Attributes.bodyDigest: BodyDigest?
    get() = getOrNull(BodyDigest.configKey)
    set(value) = value?.let { put(BodyDigest.configKey, it) } ?: remove(BodyDigest.configKey)

public inline fun Attributes.bodyDigest(configure: BodyDigest.() -> Unit) {
    getOrNull(BodyDigest.configKey)?.let(configure)
        ?: put(BodyDigest.configKey, BodyDigest().apply(configure))
}

public inline fun HttpRequestBuilder.bodyDigest(block: BodyDigest.() -> Unit): Unit =
    attributes.bodyDigest(block)

