package dev.brella.ktornea.client.digest

import io.ktor.http.content.*

public interface DigestCache {
    public suspend fun digest(block: suspend (buffer: ByteArray, offset: Int, len: Int) -> Unit)

    public fun body(): OutgoingContent
    public fun dispose()
}