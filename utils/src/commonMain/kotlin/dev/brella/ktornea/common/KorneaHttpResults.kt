package dev.brella.ktornea.common

import dev.brella.kornea.errors.common.KorneaResult
import io.ktor.client.statement.*

sealed class KorneaHttpResult<T>: KorneaResult<T> {
    abstract val response: HttpResponse

    sealed class Informational: KorneaHttpResult<Nothing>(), KorneaResult.Empty {
        class Continue(override val response: HttpResponse): Informational()
        class SwitchingProtocol(override val response: HttpResponse): Informational()
        class Processing(override val response: HttpResponse): Informational()
        class Other(override val response: HttpResponse): Informational()

        override fun get(): Nothing = throw IllegalStateException("Response had status code 1xx - ${response.status}")
    }

    sealed class Success<T>: KorneaHttpResult<T>() {
        class OK<T>(private val value: T, override val response: HttpResponse): KorneaResult.Success<T>, Success<T>() {
            override fun get(): T = value
            override fun <R> mapValue(newValue: R): KorneaResult.Success<R> =
                OK(newValue, response)
        }
        class Created<T>(private val value: T, override val response: HttpResponse): KorneaResult.Success<T>, Success<T>() {
            override fun get(): T = value
            override fun <R> mapValue(newValue: R): KorneaResult.Success<R> =
                Created(newValue, response)
        }
        class Accepted(override val response: HttpResponse): KorneaResult.Empty, Success<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Response had status code 202 Accepted; no body guaranteed")
        }
        class NonAuthoritativeInformation<T>(private val value: T, override val response: HttpResponse): KorneaResult.Success<T>, Success<T>() {
            override fun get(): T = value
            override fun <R> mapValue(newValue: R): KorneaResult.Success<R> =
                NonAuthoritativeInformation(newValue, response)
        }
        class NoContent(override val response: HttpResponse): KorneaResult.Empty, Success<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Response had status code 204 No Content; no body")
        }
        class ResetContent(override val response: HttpResponse): KorneaResult.Empty, Success<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Response had status code 205 Reset Content; no body")
        }
        class PartialContent(override val response: HttpResponse): KorneaResult.Failure, Success<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Response had status code 206 Partial Content; entity not complete")
        }

        class Other(override val response: HttpResponse): KorneaResult.Empty, Success<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Response had status code ${response.status}")
        }
    }

    sealed class Redirection: KorneaHttpResult<Nothing>(), KorneaResult.Empty {
        class MultipleChoices(override val response: HttpResponse): Redirection()
        class MovedPermanently(override val response: HttpResponse): Redirection()
        class Found(override val response: HttpResponse): Redirection()
        class SeeOther(override val response: HttpResponse): Redirection()
        class NotModified(override val response: HttpResponse): Redirection()
        class UseProxy(override val response: HttpResponse): Redirection()
        class Unused(override val response: HttpResponse): Redirection()
        class TemporaryRedirect(override val response: HttpResponse): Redirection()
        class PermanentRedirect(override val response: HttpResponse): Redirection()
        class Other(override val response: HttpResponse): Redirection()

        override fun get(): Nothing = throw IllegalStateException("Response had status code 3xx - ${response.status}")
    }

    sealed class ClientError: KorneaHttpResult<Nothing>(), KorneaResult.Failure {
        class BadRequest(override val response: HttpResponse): ClientError()
        class Unauthorized(override val response: HttpResponse): ClientError()
        class PaymentRequired(override val response: HttpResponse): ClientError()
        class Forbidden(override val response: HttpResponse): ClientError()
        class NotFound(override val response: HttpResponse): ClientError()
        class MethodNotAllowed(override val response: HttpResponse): ClientError()
        class NotAcceptable(override val response: HttpResponse): ClientError()
        class ProxyAuthenticationRequired(override val response: HttpResponse): ClientError()
        class RequestTimeout(override val response: HttpResponse): ClientError()
        class Conflict(override val response: HttpResponse): ClientError()
        class Gone(override val response: HttpResponse): ClientError()
        class LengthRequired(override val response: HttpResponse): ClientError()
        class PreconditionFailed(override val response: HttpResponse): ClientError()
        class RequestEntityTooLarge(override val response: HttpResponse): ClientError()
        class RequestURITooLong(override val response: HttpResponse): ClientError()
        class UnsupportedMediaType(override val response: HttpResponse): ClientError()
        class RequestedRangeNotSatisfiable(override val response: HttpResponse): ClientError()
        class ExpectationFailed(override val response: HttpResponse): ClientError()
        class ImATeapot(override val response: HttpResponse): ClientError()
        class EnhanceYourCalm(override val response: HttpResponse): ClientError()
        class UnprocessableEntity(override val response: HttpResponse): ClientError()
        class Locked(override val response: HttpResponse): ClientError()
        class FailedDependency(override val response: HttpResponse): ClientError()
        class ReservedForWebDAV(override val response: HttpResponse): ClientError()
        class UpgradeRequired(override val response: HttpResponse): ClientError()
        class PreconditionRequired(override val response: HttpResponse): ClientError()
        class TooManyRequests(override val response: HttpResponse): ClientError()
        class RequestHeaderFieldsTooLarge(override val response: HttpResponse): ClientError()
        class NoResponse(override val response: HttpResponse): ClientError()
        class RetryWith(override val response: HttpResponse): ClientError()
        class BlockedByWindowsParentalControls(override val response: HttpResponse): ClientError()
        class UnavailableForLegalReasons(override val response: HttpResponse): ClientError()
        class ClientClosedRequest(override val response: HttpResponse): ClientError()
        class Other(override val response: HttpResponse): ClientError()

        override fun get(): Nothing = throw IllegalStateException("Response had status code 4xx - ${response.status}")
    }

    sealed class ServerError: KorneaHttpResult<Nothing>(), KorneaResult.Failure {
        class InternalServerError(override val response: HttpResponse): ServerError()
        class NotImplemented(override val response: HttpResponse): ServerError()
        class BadGateway(override val response: HttpResponse): ServerError()
        class ServiceUnavailable(override val response: HttpResponse): ServerError()
        class GatewayTimeout(override val response: HttpResponse): ServerError()
        class HTTPVersionNotSupported(override val response: HttpResponse): ServerError()
        class VariantAlsoNegotiates(override val response: HttpResponse): ServerError()
        class InsufficientStorage(override val response: HttpResponse): ServerError()
        class LoopDetected(override val response: HttpResponse): ServerError()
        class BandwidthLimitExceeded(override val response: HttpResponse): ServerError()
        class NotExtended(override val response: HttpResponse): ServerError()
        class NetworkAuthenticationRequired(override val response: HttpResponse): ServerError()
        class NetworkReadTimeoutError(override val response: HttpResponse): ServerError()
        class NetworkConnectTimeoutError(override val response: HttpResponse): ServerError()
        class Other(override val response: HttpResponse): ServerError()

        override fun get(): Nothing = throw IllegalStateException("Response had status code 5xx - ${response.status}")
    }

    class Other(override val response: HttpResponse): KorneaHttpResult<Nothing>(), KorneaResult.Failure {
        override fun get(): Nothing = throw IllegalStateException("Response had status code ${response.status}")
    }
}