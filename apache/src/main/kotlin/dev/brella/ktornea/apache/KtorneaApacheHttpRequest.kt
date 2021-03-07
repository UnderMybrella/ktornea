package dev.brella.ktornea.apache

import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.cancel
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import java.net.ConnectException
import kotlin.coroutines.CoroutineContext

@OptIn(InternalCoroutinesApi::class, InternalAPI::class)
internal suspend fun CloseableHttpAsyncClient.sendRequest(
    request: KtorneaApacheRequestProducer,
    callContext: CoroutineContext,
    requestData: HttpRequestData
): HttpResponseData {
    val requestTime = GMTDate()

    val consumer = KtorneaApacheResponseConsumer(callContext, requestData)

    val callback = object : FutureCallback<Unit> {
        override fun failed(exception: Exception) {}
        override fun completed(result: Unit) {}
        override fun cancelled() {}
    }

    val future = execute(request, consumer, callback)!!
    try {
        val rawResponse = consumer.waitForResponse()
        val statusLine = rawResponse.statusLine

        val status = HttpStatusCode(statusLine.statusCode, statusLine.reasonPhrase)
        val version = with(rawResponse.protocolVersion) {
            HttpProtocolVersion.fromValue(protocol, major, minor)
        }

        val rawHeaders = rawResponse.allHeaders.filter {
            it.name != null || it.name.isNotBlank()
        }.groupBy(
            { it.name },
            { it.value ?: "" }
        )

        val headers = HeadersImpl(rawHeaders)
        return HttpResponseData(status, requestTime, headers, version, consumer.responseChannel, callContext)
    } catch (cause: Exception) {
        future.cancel(true)
        val mappedCause = mapCause(cause, requestData)
        callContext.cancel(CancellationException("Failed to execute request.", mappedCause))
        throw mappedCause
    }
}

internal fun mapCause(exception: Exception, requestData: HttpRequestData): Exception = when {
    exception is ConnectException && exception.isTimeoutException() -> ConnectTimeoutException(requestData, exception)
    exception is java.net.SocketTimeoutException -> SocketTimeoutException(requestData, exception)
    else -> exception
}

internal fun ConnectException.isTimeoutException() = message?.contains("Timeout connecting") ?: false