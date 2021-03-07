package dev.brella.ktornea.common

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job

suspend inline fun HttpClient.makeRequest(builder: HttpRequestBuilder.() -> Unit): HttpResponse =
    request(builder)

@OptIn(InternalAPI::class)
@Suppress("DEPRECATION_ERROR")
suspend inline fun HttpClient.executeStatement(builder: HttpRequestBuilder.() -> Unit) = execute(HttpRequestBuilder().apply(builder))

/**
 * Complete [HttpResponse] and release resources.
 */
suspend fun HttpResponse.cleanup() {
    val job = coroutineContext[Job]!! as CompletableJob

    job.apply {
        complete()
        try {
            content.cancel()
        } catch (_: Throwable) {
        }
        join()
    }
}