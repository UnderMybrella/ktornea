package dev.brella.ktornea.common

import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.io.common.BinaryDataPool
import dev.brella.kornea.io.common.flow.InputFlow
import dev.brella.kornea.toolkit.common.clearToString
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

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

val INFORMATIONAL_RANGE = 100..199
val SUCCESS_RANGE = 200..299
val REDIRECTION_RANGE = 300..399
val CLIENT_ERROR_RANGE = 400..499
val SERVER_ERROR_RANGE = 500..599

suspend inline fun <reified T> HttpClient.getAsResult(url: String, builder: HttpRequestBuilder.() -> Unit = {}): KorneaResult<T> =
    getAsResult {
        url(url)
        builder()
    }

suspend inline fun <reified T> HttpClient.headAsResult(url: String, builder: HttpRequestBuilder.() -> Unit = {}): KorneaResult<T> =
    headAsResult {
        url(url)
        builder()
    }

suspend inline fun <reified T> HttpClient.deleteAsResult(url: String, builder: HttpRequestBuilder.() -> Unit = {}): KorneaResult<T> =
    deleteAsResult {
        url(url)
        builder()
    }

suspend inline fun <reified T> HttpClient.optionsAsResult(url: String, builder: HttpRequestBuilder.() -> Unit = {}): KorneaResult<T> =
    optionsAsResult {
        url(url)
        builder()
    }

suspend inline fun <reified T> HttpClient.patchAsResult(url: String, builder: HttpRequestBuilder.() -> Unit = {}): KorneaResult<T> =
    patchAsResult {
        url(url)
        builder()
    }

suspend inline fun <reified T> HttpClient.postAsResult(url: String, builder: HttpRequestBuilder.() -> Unit = {}): KorneaResult<T> =
    postAsResult {
        url(url)
        builder()
    }

suspend inline fun <reified T> HttpClient.putAsResult(url: String, builder: HttpRequestBuilder.() -> Unit = {}): KorneaResult<T> =
    putAsResult {
        url(url)
        builder()
    }


suspend inline fun <reified T> HttpClient.getAsResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<T> =
    responseAsResult {
        method = HttpMethod.Get
        builder()
    }

suspend inline fun <reified T> HttpClient.headAsResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<T> =
    responseAsResult {
        method = HttpMethod.Head
        builder()
    }

suspend inline fun <reified T> HttpClient.deleteAsResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<T> =
    responseAsResult {
        method = HttpMethod.Delete
        builder()
    }

suspend inline fun <reified T> HttpClient.optionsAsResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<T> =
    responseAsResult {
        method = HttpMethod.Options
        builder()
    }

suspend inline fun <reified T> HttpClient.patchAsResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<T> =
    responseAsResult {
        method = HttpMethod.Patch
        builder()
    }

suspend inline fun <reified T> HttpClient.postAsResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<T> =
    responseAsResult {
        method = HttpMethod.Post
        builder()
    }

suspend inline fun <reified T> HttpClient.putAsResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<T> =
    responseAsResult {
        method = HttpMethod.Put
        builder()
    }

suspend inline fun <reified T> HttpClient.responseAsResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<T> {
    try {
        val call = executeStatement {
            builder()
            expectSuccess = false
        }

        val response = call.response

        return when (response.status) {
            HttpStatusCode.Continue -> KorneaHttpResult.Informational.Continue(response)
            HttpStatusCode.SwitchingProtocols -> KorneaHttpResult.Informational.SwitchingProtocol(response)
            HttpStatusCode.Processing -> KorneaHttpResult.Informational.Processing(response)

            HttpStatusCode.OK -> KorneaHttpResult.Success.OK(response.receive<T>(), response)
            HttpStatusCode.Created -> KorneaHttpResult.Success.Created(response.receive<T>(), response)
            HttpStatusCode.Accepted -> KorneaHttpResult.Success.Accepted(response)
            HttpStatusCode.NonAuthoritativeInformation -> KorneaHttpResult.Success.NonAuthoritativeInformation(response.receive<T>(), response)
            HttpStatusCode.NoContent -> KorneaHttpResult.Success.NoContent(response)
            HttpStatusCode.ResetContent -> KorneaHttpResult.Success.ResetContent(response)
            HttpStatusCode.PartialContent -> KorneaHttpResult.Success.PartialContent(response)

            HttpStatusCode.MultipleChoices -> KorneaHttpResult.Redirection.MultipleChoices(response)
            HttpStatusCode.MovedPermanently -> KorneaHttpResult.Redirection.MovedPermanently(response)
            HttpStatusCode.Found -> KorneaHttpResult.Redirection.Found(response)
            HttpStatusCode.SeeOther -> KorneaHttpResult.Redirection.SeeOther(response)
            HttpStatusCode.NotModified -> KorneaHttpResult.Redirection.NotModified(response)
            HttpStatusCode.UseProxy -> KorneaHttpResult.Redirection.UseProxy(response)
            HttpStatusCode.TemporaryRedirect -> KorneaHttpResult.Redirection.TemporaryRedirect(response)
            HttpStatusCode.PermanentRedirect -> KorneaHttpResult.Redirection.PermanentRedirect(response)

            HttpStatusCode.BadRequest -> KorneaHttpResult.ClientError.BadRequest(response)
            HttpStatusCode.Unauthorized -> KorneaHttpResult.ClientError.Unauthorized(response)
            HttpStatusCode.PaymentRequired -> KorneaHttpResult.ClientError.PaymentRequired(response)
            HttpStatusCode.Forbidden -> KorneaHttpResult.ClientError.Forbidden(response)
            HttpStatusCode.NotFound -> KorneaHttpResult.ClientError.NotFound(response)
            HttpStatusCode.MethodNotAllowed -> KorneaHttpResult.ClientError.MethodNotAllowed(response)
            HttpStatusCode.NotAcceptable -> KorneaHttpResult.ClientError.NotAcceptable(response)
            HttpStatusCode.ProxyAuthenticationRequired -> KorneaHttpResult.ClientError.ProxyAuthenticationRequired(response)
            HttpStatusCode.RequestTimeout -> KorneaHttpResult.ClientError.RequestTimeout(response)
            HttpStatusCode.Conflict -> KorneaHttpResult.ClientError.Conflict(response)
            HttpStatusCode.Gone -> KorneaHttpResult.ClientError.Gone(response)
            HttpStatusCode.LengthRequired -> KorneaHttpResult.ClientError.LengthRequired(response)
            HttpStatusCode.PreconditionFailed -> KorneaHttpResult.ClientError.PreconditionFailed(response)
            HttpStatusCode.RequestURITooLong -> KorneaHttpResult.ClientError.RequestURITooLong(response)
            HttpStatusCode.UnsupportedMediaType -> KorneaHttpResult.ClientError.UnsupportedMediaType(response)
            HttpStatusCode.RequestedRangeNotSatisfiable -> KorneaHttpResult.ClientError.RequestedRangeNotSatisfiable(response)
            HttpStatusCode.ExpectationFailed -> KorneaHttpResult.ClientError.ExpectationFailed(response)
            HttpStatusCode.UnprocessableEntity -> KorneaHttpResult.ClientError.UnprocessableEntity(response)
            HttpStatusCode.Locked -> KorneaHttpResult.ClientError.Locked(response)
            HttpStatusCode.FailedDependency -> KorneaHttpResult.ClientError.FailedDependency(response)
            HttpStatusCode.UpgradeRequired -> KorneaHttpResult.ClientError.UpgradeRequired(response)
            HttpStatusCode.PreconditionFailed -> KorneaHttpResult.ClientError.PreconditionFailed(response)
            HttpStatusCode.TooManyRequests -> KorneaHttpResult.ClientError.TooManyRequests(response)
            HttpStatusCode.RequestHeaderFieldTooLarge -> KorneaHttpResult.ClientError.RequestHeaderFieldsTooLarge(response)

            HttpStatusCode.InternalServerError -> KorneaHttpResult.ServerError.InternalServerError(response)
            HttpStatusCode.NotImplemented -> KorneaHttpResult.ServerError.NotImplemented(response)
            HttpStatusCode.BadGateway -> KorneaHttpResult.ServerError.BadGateway(response)
            HttpStatusCode.ServiceUnavailable -> KorneaHttpResult.ServerError.ServiceUnavailable(response)
            HttpStatusCode.GatewayTimeout -> KorneaHttpResult.ServerError.GatewayTimeout(response)
            HttpStatusCode.VariantAlsoNegotiates -> KorneaHttpResult.ServerError.VariantAlsoNegotiates(response)
            HttpStatusCode.InsufficientStorage -> KorneaHttpResult.ServerError.InsufficientStorage(response)

            else -> when (response.status.value) {
                306 -> KorneaHttpResult.Redirection.Unused(response)

                413 -> KorneaHttpResult.ClientError.RequestEntityTooLarge(response)
                418 -> KorneaHttpResult.ClientError.ImATeapot(response)
                420 -> KorneaHttpResult.ClientError.EnhanceYourCalm(response)
                425 -> KorneaHttpResult.ClientError.ReservedForWebDAV(response)
                428 -> KorneaHttpResult.ClientError.PreconditionRequired(response)
                444 -> KorneaHttpResult.ClientError.NoResponse(response)
                449 -> KorneaHttpResult.ClientError.RetryWith(response)
                450 -> KorneaHttpResult.ClientError.BlockedByWindowsParentalControls(response)
                451 -> KorneaHttpResult.ClientError.UnavailableForLegalReasons(response)
                499 -> KorneaHttpResult.ClientError.ClientClosedRequest(response)

                505 -> KorneaHttpResult.ServerError.HTTPVersionNotSupported(response)
                508 -> KorneaHttpResult.ServerError.LoopDetected(response)
                509 -> KorneaHttpResult.ServerError.BandwidthLimitExceeded(response)
                510 -> KorneaHttpResult.ServerError.NotExtended(response)
                598 -> KorneaHttpResult.ServerError.NetworkReadTimeoutError(response)
                599 -> KorneaHttpResult.ServerError.NetworkConnectTimeoutError(response)

                in INFORMATIONAL_RANGE -> KorneaHttpResult.Informational.Other(response)
                in SUCCESS_RANGE -> KorneaHttpResult.Success.Other(response)
                in REDIRECTION_RANGE -> KorneaHttpResult.Redirection.Other(response)
                in CLIENT_ERROR_RANGE -> KorneaHttpResult.ClientError.Other(response)
                in SERVER_ERROR_RANGE -> KorneaHttpResult.ServerError.Other(response)

                else -> KorneaHttpResult.Other(response)
            }
        }
    } catch (th: Throwable) {
        return KorneaResult.thrown(th)
    }
}

suspend inline fun HttpClient.stream(builder: HttpRequestBuilder.() -> Unit): Flow<String>? =
    executeStatement(builder).let { call ->
        val response = call.response

        if (response.contentType()?.match("text/event-stream") != true) {
            response.cleanup()
            return@let null
        } else channelFlow<String> {
            val pool = BinaryDataPool(null, null, 1)
            val content = response.content
            val buffer = ByteArray(8192)

            val binaryInput = pool.openInputFlow().get()
            val binaryOutput = pool.openOutputFlow().get()

            val readContent = launch {
                while (isActive && !content.isClosedForRead) {
                    if (content.availableForRead > 0) {
                        val read = content.readAvailable(buffer)
                        binaryOutput.write(buffer, 0, read)
                    }

                    yield()
                }
            }

            val writeContent = launch {
                val content = content
                val builder = StringBuilder()
                var c: Char?

                while (isActive) {
                    c = binaryInput.readUtf8Character()

                    when (c) {
                        null -> {
                            if (!readContent.isActive) {
                                if (builder.isNotBlank()) send(builder.clearToString())
                                break
                            }

                            yield()
                            continue
                        }
                        '\n' -> if (builder.isNotBlank()) send(builder.clearToString())
                        else -> builder.append(c)
                    }
                }
            }

            joinAll(readContent, writeContent)

            response.cleanup()
        }
    }

@OptIn(ExperimentalCoroutinesApi::class)
suspend inline fun HttpClient.streamAsResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<Flow<String>> {
    try {
        val call = executeStatement {
            builder()
            expectSuccess = false
        }

        val response = call.response

        return when (response.status) {
            HttpStatusCode.OK,
            HttpStatusCode.Created,
            HttpStatusCode.NonAuthoritativeInformation -> {
                val flow = channelFlow<String> {
                    val pool = BinaryDataPool(null, null, 1)
                    val content = response.content
                    val buffer = ByteArray(8192)

                    val binaryInput = pool.openInputFlow().get()
                    val binaryOutput = pool.openOutputFlow().get()

                    val readContent = launch {
                        while (isActive && !content.isClosedForRead) {
                            if (content.availableForRead > 0) {
                                val read = content.readAvailable(buffer)
                                binaryOutput.write(buffer, 0, read)
                            }

                            yield()
                        }
                    }

                    val writeContent = launch {
                        val content = content
                        val builder = StringBuilder()
                        var c: Char?

                        while (isActive) {
                            c = binaryInput.readUtf8Character()

                            when (c) {
                                null -> {
                                    if (!readContent.isActive) {
                                        if (builder.isNotBlank()) send(builder.clearToString())
                                        break
                                    }

                                    yield()
                                    continue
                                }
                                '\n' -> if (builder.isNotBlank()) send(builder.clearToString())
                                else -> builder.append(c)
                            }
                        }
                    }

                    joinAll(readContent, writeContent)

                    response.cleanup()
                }

                when (response.status) {
                    HttpStatusCode.OK -> KorneaHttpResult.Success.OK(flow, response)
                    HttpStatusCode.Created -> KorneaHttpResult.Success.Created(flow, response)
                    HttpStatusCode.NonAuthoritativeInformation -> KorneaHttpResult.Success.NonAuthoritativeInformation(flow, response)

                    else -> TODO("Status: ${response.status}")
                }
            }

            else -> try {
                when (response.status) {
                    HttpStatusCode.Continue -> KorneaHttpResult.Informational.Continue(response)
                    HttpStatusCode.SwitchingProtocols -> KorneaHttpResult.Informational.SwitchingProtocol(response)
                    HttpStatusCode.Processing -> KorneaHttpResult.Informational.Processing(response)

                    HttpStatusCode.Accepted -> KorneaHttpResult.Success.Accepted(response)
                    HttpStatusCode.NoContent -> KorneaHttpResult.Success.NoContent(response)
                    HttpStatusCode.ResetContent -> KorneaHttpResult.Success.ResetContent(response)
                    HttpStatusCode.PartialContent -> KorneaHttpResult.Success.PartialContent(response)

                    HttpStatusCode.MultipleChoices -> KorneaHttpResult.Redirection.MultipleChoices(response)
                    HttpStatusCode.MovedPermanently -> KorneaHttpResult.Redirection.MovedPermanently(response)
                    HttpStatusCode.Found -> KorneaHttpResult.Redirection.Found(response)
                    HttpStatusCode.SeeOther -> KorneaHttpResult.Redirection.SeeOther(response)
                    HttpStatusCode.NotModified -> KorneaHttpResult.Redirection.NotModified(response)
                    HttpStatusCode.UseProxy -> KorneaHttpResult.Redirection.UseProxy(response)
                    HttpStatusCode.TemporaryRedirect -> KorneaHttpResult.Redirection.TemporaryRedirect(response)
                    HttpStatusCode.PermanentRedirect -> KorneaHttpResult.Redirection.PermanentRedirect(response)

                    HttpStatusCode.BadRequest -> KorneaHttpResult.ClientError.BadRequest(response)
                    HttpStatusCode.Unauthorized -> KorneaHttpResult.ClientError.Unauthorized(response)
                    HttpStatusCode.PaymentRequired -> KorneaHttpResult.ClientError.PaymentRequired(response)
                    HttpStatusCode.Forbidden -> KorneaHttpResult.ClientError.Forbidden(response)
                    HttpStatusCode.NotFound -> KorneaHttpResult.ClientError.NotFound(response)
                    HttpStatusCode.MethodNotAllowed -> KorneaHttpResult.ClientError.MethodNotAllowed(response)
                    HttpStatusCode.NotAcceptable -> KorneaHttpResult.ClientError.NotAcceptable(response)
                    HttpStatusCode.ProxyAuthenticationRequired -> KorneaHttpResult.ClientError.ProxyAuthenticationRequired(response)
                    HttpStatusCode.RequestTimeout -> KorneaHttpResult.ClientError.RequestTimeout(response)
                    HttpStatusCode.Conflict -> KorneaHttpResult.ClientError.Conflict(response)
                    HttpStatusCode.Gone -> KorneaHttpResult.ClientError.Gone(response)
                    HttpStatusCode.LengthRequired -> KorneaHttpResult.ClientError.LengthRequired(response)
                    HttpStatusCode.PreconditionFailed -> KorneaHttpResult.ClientError.PreconditionFailed(response)
                    HttpStatusCode.RequestURITooLong -> KorneaHttpResult.ClientError.RequestURITooLong(response)
                    HttpStatusCode.UnsupportedMediaType -> KorneaHttpResult.ClientError.UnsupportedMediaType(response)
                    HttpStatusCode.RequestedRangeNotSatisfiable -> KorneaHttpResult.ClientError.RequestedRangeNotSatisfiable(response)
                    HttpStatusCode.ExpectationFailed -> KorneaHttpResult.ClientError.ExpectationFailed(response)
                    HttpStatusCode.UnprocessableEntity -> KorneaHttpResult.ClientError.UnprocessableEntity(response)
                    HttpStatusCode.Locked -> KorneaHttpResult.ClientError.Locked(response)
                    HttpStatusCode.FailedDependency -> KorneaHttpResult.ClientError.FailedDependency(response)
                    HttpStatusCode.UpgradeRequired -> KorneaHttpResult.ClientError.UpgradeRequired(response)
                    HttpStatusCode.PreconditionFailed -> KorneaHttpResult.ClientError.PreconditionFailed(response)
                    HttpStatusCode.TooManyRequests -> KorneaHttpResult.ClientError.TooManyRequests(response)
                    HttpStatusCode.RequestHeaderFieldTooLarge -> KorneaHttpResult.ClientError.RequestHeaderFieldsTooLarge(response)

                    HttpStatusCode.InternalServerError -> KorneaHttpResult.ServerError.InternalServerError(response)
                    HttpStatusCode.NotImplemented -> KorneaHttpResult.ServerError.NotImplemented(response)
                    HttpStatusCode.BadGateway -> KorneaHttpResult.ServerError.BadGateway(response)
                    HttpStatusCode.ServiceUnavailable -> KorneaHttpResult.ServerError.ServiceUnavailable(response)
                    HttpStatusCode.GatewayTimeout -> KorneaHttpResult.ServerError.GatewayTimeout(response)
                    HttpStatusCode.VariantAlsoNegotiates -> KorneaHttpResult.ServerError.VariantAlsoNegotiates(response)
                    HttpStatusCode.InsufficientStorage -> KorneaHttpResult.ServerError.InsufficientStorage(response)

                    else -> when (response.status.value) {
                        306 -> KorneaHttpResult.Redirection.Unused(response)

                        413 -> KorneaHttpResult.ClientError.RequestEntityTooLarge(response)
                        418 -> KorneaHttpResult.ClientError.ImATeapot(response)
                        420 -> KorneaHttpResult.ClientError.EnhanceYourCalm(response)
                        425 -> KorneaHttpResult.ClientError.ReservedForWebDAV(response)
                        428 -> KorneaHttpResult.ClientError.PreconditionRequired(response)
                        444 -> KorneaHttpResult.ClientError.NoResponse(response)
                        449 -> KorneaHttpResult.ClientError.RetryWith(response)
                        450 -> KorneaHttpResult.ClientError.BlockedByWindowsParentalControls(response)
                        451 -> KorneaHttpResult.ClientError.UnavailableForLegalReasons(response)
                        499 -> KorneaHttpResult.ClientError.ClientClosedRequest(response)

                        505 -> KorneaHttpResult.ServerError.HTTPVersionNotSupported(response)
                        508 -> KorneaHttpResult.ServerError.LoopDetected(response)
                        509 -> KorneaHttpResult.ServerError.BandwidthLimitExceeded(response)
                        510 -> KorneaHttpResult.ServerError.NotExtended(response)
                        598 -> KorneaHttpResult.ServerError.NetworkReadTimeoutError(response)
                        599 -> KorneaHttpResult.ServerError.NetworkConnectTimeoutError(response)

                        in INFORMATIONAL_RANGE -> KorneaHttpResult.Informational.Other(response)
                        in SUCCESS_RANGE -> KorneaHttpResult.Success.Other(response)
                        in REDIRECTION_RANGE -> KorneaHttpResult.Redirection.Other(response)
                        in CLIENT_ERROR_RANGE -> KorneaHttpResult.ClientError.Other(response)
                        in SERVER_ERROR_RANGE -> KorneaHttpResult.ServerError.Other(response)

                        else -> KorneaHttpResult.Other(response)
                    }
                }
            } finally {
                response.cleanup()
            }
        }
    } catch (th: Throwable) {
        return KorneaResult.thrown(th)
    }
}

suspend fun InputFlow.readUtf8Character(): Char? {
    val a = read() ?: return null

    when {
        a and 0xF0 == 0xF0 -> {
            val b = read() ?: return null
            val c = read() ?: return null
            val d = read() ?: return null

            return (((a and 0xF) shl 18) or
                    ((b and 0x3F) shl 12) or
                    ((c and 0x3F) shl 6) or
                    ((d and 0x3F) shl 0)).toChar()
        }
        a and 0xE0 == 0xE0 -> {
            val b = read() ?: return null
            val c = read() ?: return null

            return (((a and 0xF) shl 12) or
                    ((b and 0x3F) shl 6) or
                    ((c and 0x3F) shl 0)).toChar()
        }
        a and 0xC0 == 0xC0 -> {
            val b = read() ?: return null

            return (((a and 0xF) shl 6) or
                    ((b and 0x3F) shl 0)).toChar()
        }
        a and 0x80 == 0x80 -> return null
        else -> return a.toChar()
    }
}