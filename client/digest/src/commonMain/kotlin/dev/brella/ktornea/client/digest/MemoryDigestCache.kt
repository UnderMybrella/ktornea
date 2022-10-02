package dev.brella.ktornea.client.digest

import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

public class MemoryDigestCache(
    delegate: OutgoingContent,
    private val callContext: CoroutineContext,
) : DigestCache, OutgoingContent.WriteChannelContent() {
    @OptIn(DelicateCoroutinesApi::class)
    private val content: Deferred<ByteArray> = when (delegate) {
        is ByteArrayContent -> CompletableDeferred(delegate.bytes())
        is ProtocolUpgrade -> throw UnsupportedContentTypeException(delegate)
        is NoContent -> CompletableDeferred(ByteArray(0))
        is ReadChannelContent -> GlobalScope.async { delegate.readFrom().toByteArray() }
        is WriteChannelContent -> GlobalScope.async {
            val byteChannel = ByteChannel(true)
            delegate.writeTo(byteChannel)
            byteChannel.toByteArray()
        }
    }

    @Suppress("CanBePrimaryConstructorProperty") // required to avoid InvalidMutabilityException on native
    private val delegate = delegate

    override val contentType: ContentType?
        get() = delegate.contentType
    override val contentLength: Long?
        get() = delegate.contentLength
    override val status: HttpStatusCode?
        get() = delegate.status
    override val headers: Headers
        get() = delegate.headers

    override fun <T : Any> getProperty(key: AttributeKey<T>): T? = delegate.getProperty(key)
    override fun <T : Any> setProperty(key: AttributeKey<T>, value: T?): Unit = delegate.setProperty(key, value)

    override suspend fun digest(block: suspend (buffer: ByteArray, offset: Int, len: Int) -> Unit) {
        val body = this.content.await()
        block(body, 0, body.size)
    }

    override fun body(): OutgoingContent.WriteChannelContent = this

    override fun dispose() {
        content.cancel()
    }

    override suspend fun writeTo(channel: ByteWriteChannel) {
        channel.writeFully(content.await())
    }
}