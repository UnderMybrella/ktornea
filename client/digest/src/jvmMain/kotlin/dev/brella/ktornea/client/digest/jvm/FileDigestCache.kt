/*
 * Copyright 2014-2022 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.brella.ktornea.client.digest.jvm

import dev.brella.ktornea.client.digest.DigestCache
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

public class FileDigestCache(
    delegate: OutgoingContent,
    bufferSize: Int,
    context: CoroutineContext
): DigestCache, OutgoingContent.ReadChannelContent() {
    private val file = File.createTempFile("ktornea-digest-cache", ".tmp")
    private var body: ByteReadChannel? = null

    @OptIn(DelicateCoroutinesApi::class)
    private val saveJob = GlobalScope.launch(context + Dispatchers.IO) {
        @Suppress("BlockingMethodInNonBlockingContext")
        FileOutputStream(file).use { stream ->
            stream.channel.use { out ->
                out.truncate(0L)

                when (delegate) {
                    is ByteArrayContent -> out.write(ByteBuffer.wrap(delegate.bytes()))
                    is NoContent -> {}
                    is ProtocolUpgrade -> throw UnsupportedContentTypeException(delegate)
                    is ReadChannelContent -> {
                        body = delegate.readFrom()
                    }
                    is WriteChannelContent -> {
                        body = writer(context, autoFlush = true) {
                            delegate.writeTo(channel)
                        }.channel
                    }
                }

                body?.let { body ->
                    val buffer = ByteBuffer.allocate(bufferSize)
                    buffer.position(buffer.limit())

                    while (isActive) {
                        while (buffer.hasRemaining()) {
                            out.write(buffer)
                        }
                        buffer.clear()

                        if (body.readAvailable(buffer) == -1) break
                        buffer.flip()
                    }
                }
            }
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

    override fun body(): OutgoingContent =
        this

    override fun readFrom(): ByteReadChannel =
        file.readChannel()

    override suspend fun digest(block: suspend (buffer: ByteArray, offset: Int, len: Int) -> Unit) {
        val buffer = ByteArray(8192)
        val channel = file.readChannel()
        var read = -1
        while (true) {
            read = channel.readAvailable(buffer)
            if (read == -1) break

            block(buffer, 0, read)
        }
    }

    override fun dispose() {
        runCatching {
            saveJob.cancel()
        }
        runCatching {
            file.delete()
        }

        if (body?.isClosedForRead == false) {
            runCatching {
                body?.cancel()
            }
        }
    }
}