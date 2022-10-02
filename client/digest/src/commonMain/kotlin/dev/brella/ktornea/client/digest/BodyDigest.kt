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

public typealias BodyDigestCacheConstructor = PipelineContext<Any, HttpRequestBuilder>.(OutgoingContent, CoroutineContext) -> DigestCache

public interface BodyDigestListener {
    public companion object {
        public operator fun invoke(
            begin: suspend PipelineContext<Any, HttpRequestBuilder>.(content: OutgoingContent) -> Boolean = { true },
            update: suspend (buffer: ByteArray, offset: Int, length: Int) -> Unit = { _, _, _ -> },
            end: suspend PipelineContext<Any, HttpRequestBuilder>.() -> Unit = { },
        ): BodyDigestListener =
            Base(begin, update, end)
    }

    private data class Base(
        val begin: suspend PipelineContext<Any, HttpRequestBuilder>.(content: OutgoingContent) -> Boolean = { true },
        val update: suspend (buffer: ByteArray, offset: Int, length: Int) -> Unit = { _, _, _ -> },
        val end: suspend PipelineContext<Any, HttpRequestBuilder>.() -> Unit = { },
    ) : BodyDigestListener {
        override suspend fun PipelineContext<Any, HttpRequestBuilder>.begin(content: OutgoingContent): Boolean =
            begin.invoke(this, content)

        override suspend fun update(buffer: ByteArray, offset: Int, length: Int) =
            update.invoke(buffer, offset, length)

        override suspend fun PipelineContext<Any, HttpRequestBuilder>.end() =
            end.invoke(this)
    }

    public suspend fun PipelineContext<Any, HttpRequestBuilder>.begin(content: OutgoingContent): Boolean {
        return true
    }

    public suspend fun update(buffer: ByteArray, offset: Int, length: Int) {

    }

    public suspend fun PipelineContext<Any, HttpRequestBuilder>.end() {

    }
}

private suspend inline fun BodyDigestListener.begin(
    context: PipelineContext<Any, HttpRequestBuilder>,
    content: OutgoingContent,
): Boolean =
    context.begin(content)

private suspend inline fun BodyDigestListener.end(context: PipelineContext<Any, HttpRequestBuilder>) =
    context.end()

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
                if (content !is OutgoingContent) return@intercept

                val localConfig = context.attributes
                    .getOrNull(configKey)

                if (plugin.listeners.isEmpty() && localConfig?.listeners.isNullOrEmpty()) return@intercept

                val digestContent = (localConfig?.cacheConstructor ?: plugin.cacheConstructor)
                    ?.invoke(this, content, context.executionContext)
                    ?: MemoryDigestCache(content, context.executionContext)

                plugin.listeners.forEach { listener ->
                    if (listener.begin(this, digestContent.body())) {
                        digestContent.digest(listener::update)

                        listener.end(this)
                    }
                }

                localConfig?.listeners?.forEach { listener ->
                    if (listener.begin(this, digestContent.body())) {
                        digestContent.digest(listener::update)

                        listener.end(this)
                    }
                }

                proceedWith(digestContent.body())
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

