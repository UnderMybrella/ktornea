package dev.brella.ktornea.common

import dev.brella.kornea.base.common.ListRingBuffer
import dev.brella.kornea.base.common.Optional
import dev.brella.kornea.base.common.RingBuffer
import dev.brella.kornea.base.common.doOnPresent
import dev.brella.kornea.base.common.empty
import dev.brella.kornea.base.common.getOrElseRun
import dev.brella.kornea.base.common.map
import dev.brella.kornea.base.common.of
import dev.brella.kornea.errors.common.KorneaResult
import io.ktor.client.statement.*
import kotlinx.atomicfu.atomic

sealed class KorneaHttpResult<T>() : KorneaResult<T> {
    companion object DefaultPools {
        const val DEFAULT_MAX_CAPACITY = 100

        internal inline fun <reified T> ringBuffer(): Lazy<RingBuffer<T>> = lazy { ListRingBuffer.withCapacity<T>(DEFAULT_MAX_CAPACITY) }

        internal val CONTINUE by ringBuffer<Informational.Continue>()
        internal val SWITCHING_PROTOCOL by ringBuffer<Informational.SwitchingProtocol>()
        internal val PROCESSING by ringBuffer<Informational.Processing>()
        internal val INFORMATIONAL_OTHER by ringBuffer<Informational.Other>()

        internal val OK by ringBuffer<Success.OK<Any?>>()
        internal val CREATED by ringBuffer<Success.Created<Any?>>()
        internal val ACCEPTED by ringBuffer<Success.Accepted>()
        internal val NON_AUTHORITATIVE_INFORMATION by ringBuffer<Success.NonAuthoritativeInformation<Any?>>()
        internal val NO_CONTENT by ringBuffer<Success.NoContent>()
        internal val RESET_CONTENT by ringBuffer<Success.ResetContent>()
        internal val PARTIAL_CONTENT by ringBuffer<Success.PartialContent>()
        internal val SUCCESS_OTHER by ringBuffer<Success.Other>()

        internal val MULTIPLE_CHOICES by ringBuffer<Redirection.MultipleChoices>()
        internal val MOVED_PERMANENTLY by ringBuffer<Redirection.MovedPermanently>()
        internal val FOUND by ringBuffer<Redirection.Found>()
        internal val SEE_OTHER by ringBuffer<Redirection.SeeOther>()
        internal val NOT_MODIFIED by ringBuffer<Redirection.NotModified>()
        internal val USE_PROXY by ringBuffer<Redirection.UseProxy>()
        internal val REDIRECTION_UNUSED by ringBuffer<Redirection.Unused>()
        internal val TEMPORARY_REDIRECT by ringBuffer<Redirection.TemporaryRedirect>()
        internal val PERMANENT_REDIRECT by ringBuffer<Redirection.PermanentRedirect>()
        internal val REDIRECTION_OTHER by ringBuffer<Redirection.Other>()

        internal val BAD_REQUEST by ringBuffer<ClientError.BadRequest>()
        internal val UNAUTHORIZED by ringBuffer<ClientError.Unauthorized>()
        internal val PAYMENT_REQUIRED by ringBuffer<ClientError.PaymentRequired>()
        internal val FORBIDDEN by ringBuffer<ClientError.Forbidden>()
        internal val NOT_FOUND by ringBuffer<ClientError.NotFound>()
        internal val METHOD_NOT_ALLOWED by ringBuffer<ClientError.MethodNotAllowed>()
        internal val NOT_ACCEPTABLE by ringBuffer<ClientError.NotAcceptable>()
        internal val PROXY_AUTHENTICATION_REQUIRED by ringBuffer<ClientError.ProxyAuthenticationRequired>()
        internal val REQUEST_TIMEOUT by ringBuffer<ClientError.RequestTimeout>()
        internal val CONFLICT by ringBuffer<ClientError.Conflict>()
        internal val GONE by ringBuffer<ClientError.Gone>()
        internal val LENGTH_REQUIRED by ringBuffer<ClientError.LengthRequired>()
        internal val PRECONDITION_FAILED by ringBuffer<ClientError.PreconditionFailed>()
        internal val REQUEST_ENTITY_TOO_LARGE by ringBuffer<ClientError.RequestEntityTooLarge>()
        internal val REQUEST_URI_TOO_LONG by ringBuffer<ClientError.RequestURITooLong>()
        internal val UNSUPPORTED_MEDIA_TYPE by ringBuffer<ClientError.UnsupportedMediaType>()
        internal val REQUESTED_RANGE_NOT_SATISFIABLE by ringBuffer<ClientError.RequestedRangeNotSatisfiable>()
        internal val EXPECTATION_FAILED by ringBuffer<ClientError.ExpectationFailed>()
        internal val IM_A_TEAPOT by ringBuffer<ClientError.ImATeapot>()
        internal val ENHANCE_YOUR_CALM by ringBuffer<ClientError.EnhanceYourCalm>()
        internal val UNPROCESSABLE_ENTITY by ringBuffer<ClientError.UnprocessableEntity>()
        internal val LOCKED by ringBuffer<ClientError.Locked>()
        internal val FAILED_DEPENDENCY by ringBuffer<ClientError.FailedDependency>()
        internal val CLIENT_ERROR_RESERVED_FOR_WEBDAV by ringBuffer<ClientError.ReservedForWebDAV>()
        internal val UPGRADE_REQUIRED by ringBuffer<ClientError.UpgradeRequired>()
        internal val PRECONDITION_REQUIRED by ringBuffer<ClientError.PreconditionRequired>()
        internal val TOO_MANY_REQUESTS by ringBuffer<ClientError.TooManyRequests>()
        internal val REQUEST_HEADER_FIELDS_TOO_LARGE by ringBuffer<ClientError.RequestHeaderFieldsTooLarge>()
        internal val NO_RESPONSE by ringBuffer<ClientError.NoResponse>()
        internal val RETRY_WITH by ringBuffer<ClientError.RetryWith>()
        internal val BLOCKED_BY_WINDOWS_PARENTAL_CONTROLS by ringBuffer<ClientError.BlockedByWindowsParentalControls>()
        internal val UNAVAILABLE_FOR_LEGAL_REASONS by ringBuffer<ClientError.UnavailableForLegalReasons>()
        internal val CLIENT_CLOSED_REQUEST by ringBuffer<ClientError.ClientClosedRequest>()
        internal val CLIENT_ERROR_OTHER by ringBuffer<ClientError.Other>()

        internal val INTERNAL_SERVER_ERROR by ringBuffer<ServerError.InternalServerError>()
        internal val NOT_IMPLEMENTED by ringBuffer<ServerError.NotImplemented>()
        internal val BAD_GATEWAY by ringBuffer<ServerError.BadGateway>()
        internal val SERVICE_UNAVAILABLE by ringBuffer<ServerError.ServiceUnavailable>()
        internal val GATEWAY_TIMEOUT by ringBuffer<ServerError.GatewayTimeout>()
        internal val HTTP_VERSION_NOT_SUPPORTED by ringBuffer<ServerError.HTTPVersionNotSupported>()
        internal val VARIANT_ALSO_NEGOTIATES by ringBuffer<ServerError.VariantAlsoNegotiates>()
        internal val INSUFFICIENT_STORAGE by ringBuffer<ServerError.InsufficientStorage>()
        internal val LOOP_DETECTED by ringBuffer<ServerError.LoopDetected>()
        internal val BANDWIDTH_LIMIT_EXCEEDED by ringBuffer<ServerError.BandwidthLimitExceeded>()
        internal val NOT_EXTENDED by ringBuffer<ServerError.NotExtended>()
        internal val NETWORK_AUTHENTICATION_REQUIRED by ringBuffer<ServerError.NetworkAuthenticationRequired>()
        internal val NETWORK_READ_TIMEOUT by ringBuffer<ServerError.NetworkReadTimeoutError>()
        internal val NETWORK_CONNECT_TIMEOUT by ringBuffer<ServerError.NetworkConnectTimeoutError>()
        internal val SERVER_ERROR_OTHER by ringBuffer<ServerError.Other>()

        internal val OTHER by ringBuffer<Other>()

        fun Other(response: HttpResponse) =
            OTHER.pop()
                .doOnPresent { result -> result._response = response }
                .getOrElseRun { Other(response, OTHER) }
    }

    internal object IDLE

    internal abstract var _response: HttpResponse
    val response: HttpResponse by ::_response

    private final val _latch = atomic(1)
    protected var latch by _latch

    sealed class Informational : KorneaHttpResult<Nothing>(), KorneaResult.Empty {
        companion object {
            fun Continue(response: HttpResponse) =
                CONTINUE.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Continue(response, CONTINUE) }

            fun SwitchingProtocol(response: HttpResponse) =
                SWITCHING_PROTOCOL.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { SwitchingProtocol(response, SWITCHING_PROTOCOL) }

            fun Processing(response: HttpResponse) =
                PROCESSING.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Processing(response, PROCESSING) }

            fun Other(response: HttpResponse) =
                INFORMATIONAL_OTHER.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Other(response, INFORMATIONAL_OTHER) }
        }

        class Continue internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Continue>) : Informational() {
            override fun push() = returnTo.push(this)
        }

        class SwitchingProtocol internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<SwitchingProtocol>) : Informational() {
            override fun push() = returnTo.push(this)
        }

        class Processing internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Processing>) : Informational() {
            override fun push() = returnTo.push(this)
        }

        class Other internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Other>) : Informational() {
            override fun push() = returnTo.push(this)
        }

        override fun get(): Nothing = throw IllegalStateException("Response had status code 1xx - ${_response.status}")

        override fun dataHashCode(): Optional<Int> =
            super<KorneaHttpResult>.dataHashCode()

        override fun isAvailable(dataHashCode: Int?): Boolean? =
            super<KorneaHttpResult>.isAvailable(dataHashCode)

        override fun consume(dataHashCode: Int?) =
            super<KorneaHttpResult>.consume(dataHashCode)
    }

    sealed class Success<T> : KorneaHttpResult<T>() {
        companion object {
            fun <T> OK(value: T, response: HttpResponse) =
                OK.pop()
                    .map { result -> result.replace(value, response) }
                    .getOrElseRun { OK(value, response, OK) }

            fun <T> Created(value: T, response: HttpResponse) =
                CREATED.pop()
                    .map { result -> result.replace(value, response) }
                    .getOrElseRun { Created(value, response, CREATED) }

            fun Accepted(response: HttpResponse) =
                ACCEPTED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Accepted(response, ACCEPTED) }

            fun <T> NonAuthoritativeInformation(value: T, response: HttpResponse) =
                NON_AUTHORITATIVE_INFORMATION.pop()
                    .map { result -> result.replace(value, response) }
                    .getOrElseRun { NonAuthoritativeInformation(value, response, NON_AUTHORITATIVE_INFORMATION) }

            fun NoContent(response: HttpResponse) =
                NO_CONTENT.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { NoContent(response, NO_CONTENT) }

            fun ResetContent(response: HttpResponse) =
                RESET_CONTENT.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { ResetContent(response, RESET_CONTENT) }

            fun PartialContent(response: HttpResponse) =
                PARTIAL_CONTENT.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { PartialContent(response, PARTIAL_CONTENT) }

            fun Other(response: HttpResponse) =
                SUCCESS_OTHER.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Other(response, SUCCESS_OTHER) }
        }

        class OK<T> internal constructor(private var value: Any?, override var _response: HttpResponse, internal val returnTo: RingBuffer<OK<Any?>>) : KorneaResult.Success<T>, Success<T>() {
            override fun get(): T = if (value === IDLE) throw IllegalStateException("PooledResult<T> was closed") else value as T
            override fun <R> mapValue(newValue: R): KorneaResult<R> =
                if (latch <= 1) {
                    this.value = newValue
                    this as KorneaResult<R>
                } else {
                    latch -= 1
                    OK(newValue, _response, returnTo)
                }

            fun <R> replace(newValue: R, newResponse: HttpResponse): OK<R> {
                value = newValue
                _response = newResponse

                return this as OK<R>
            }

            override fun dataHashCode(): Optional<Int> =
                if (value === IDLE) Optional.empty() else Optional.of(_response.hashCode() * 31 + value.hashCode())

            override fun push() {
                value = IDLE
                returnTo.push(this as OK<Any?>)
            }
        }

        class Created<T> internal constructor(private var value: Any?, override var _response: HttpResponse, internal val returnTo: RingBuffer<Created<Any?>>) : KorneaResult.Success<T>, Success<T>() {
            override fun get(): T = if (value === IDLE) throw IllegalStateException("PooledResult<T> was closed") else value as T
            override fun <R> mapValue(newValue: R): KorneaResult<R> =
                if (latch <= 1) {
                    this.value = newValue
                    this as KorneaResult<R>
                } else {
                    latch -= 1
                    Created(newValue, _response, returnTo)
                }

            fun <R> replace(newValue: R, newResponse: HttpResponse): Created<R> {
                value = newValue
                _response = newResponse

                return this as Created<R>
            }

            override fun dataHashCode(): Optional<Int> =
                if (value === IDLE) Optional.empty() else Optional.of(_response.hashCode() * 31 + value.hashCode())

            override fun push() {
                value = IDLE
                returnTo.push(this as Created<Any?>)
            }
        }

        class Accepted internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Accepted>) : KorneaResult.Empty, Success<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Response had status code 202 Accepted; no body guaranteed")

            override fun dataHashCode(): Optional<Int> =
                super<Success>.dataHashCode()

            override fun isAvailable(dataHashCode: Int?): Boolean? =
                super<Success>.isAvailable(dataHashCode)

            override fun consume(dataHashCode: Int?) =
                super<Success>.consume(dataHashCode)

            override fun push() = returnTo.push(this)
        }

        class NonAuthoritativeInformation<T> internal constructor(private var value: Any?, override var _response: HttpResponse, internal val returnTo: RingBuffer<NonAuthoritativeInformation<Any?>>) : KorneaResult.Success<T>, Success<T>() {
            override fun get(): T = if (value === IDLE) throw IllegalStateException("PooledResult<T> was closed") else value as T

            //                if (value === IDLE) return KorneaResult.Empty.ofClosed()
            override fun <R> mapValue(newValue: R): KorneaResult<R> =
                if (latch <= 1) {
                    this.value = newValue
                    this as KorneaResult<R>
                } else {
                    latch -= 1
                    NonAuthoritativeInformation(newValue, _response, returnTo)
                }

            fun <R> replace(newValue: R, newResponse: HttpResponse): NonAuthoritativeInformation<R> {
                value = newValue
                _response = newResponse

                return this as NonAuthoritativeInformation<R>
            }

            override fun dataHashCode(): Optional<Int> =
                if (value === IDLE) Optional.empty() else Optional.of(_response.hashCode() * 31 + value.hashCode())

            override fun push() {
                value = IDLE
                returnTo.push(this as NonAuthoritativeInformation<Any?>)
            }
        }

        class NoContent internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<NoContent>) : KorneaResult.Empty, Success<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Response had status code 204 No Content; no body")

            override fun dataHashCode(): Optional<Int> =
                super<Success>.dataHashCode()

            override fun isAvailable(dataHashCode: Int?): Boolean? =
                super<Success>.isAvailable(dataHashCode)

            override fun consume(dataHashCode: Int?) =
                super<Success>.consume(dataHashCode)

            override fun push() = returnTo.push(this)
        }

        class ResetContent internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<ResetContent>) : KorneaResult.Empty, Success<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Response had status code 205 Reset Content; no body")

            override fun dataHashCode(): Optional<Int> =
                super<Success>.dataHashCode()

            override fun isAvailable(dataHashCode: Int?): Boolean? =
                super<Success>.isAvailable(dataHashCode)

            override fun consume(dataHashCode: Int?) =
                super<Success>.consume(dataHashCode)

            override fun push() = returnTo.push(this)
        }

        class PartialContent internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<PartialContent>) : KorneaResult.Failure, Success<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Response had status code 206 Partial Content; entity not complete")

            override fun dataHashCode(): Optional<Int> =
                super<Success>.dataHashCode()

            override fun isAvailable(dataHashCode: Int?): Boolean? =
                super<Success>.isAvailable(dataHashCode)

            override fun consume(dataHashCode: Int?) =
                super<Success>.consume(dataHashCode)

            override fun push() = returnTo.push(this)
        }

        class Other internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Other>) : KorneaResult.Empty, Success<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Response had status code ${_response.status}")

            override fun dataHashCode(): Optional<Int> =
                super<Success>.dataHashCode()

            override fun isAvailable(dataHashCode: Int?): Boolean? =
                super<Success>.isAvailable(dataHashCode)

            override fun consume(dataHashCode: Int?) =
                super<Success>.consume(dataHashCode)

            override fun push() = returnTo.push(this)
        }
    }

    sealed class Redirection : KorneaHttpResult<Nothing>(), KorneaResult.Empty {
        companion object {
            fun MultipleChoices(response: HttpResponse) =
                MULTIPLE_CHOICES.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { MultipleChoices(response, MULTIPLE_CHOICES) }

            fun MovedPermanently(response: HttpResponse) =
                MOVED_PERMANENTLY.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { MovedPermanently(response, MOVED_PERMANENTLY) }

            fun Found(response: HttpResponse) =
                FOUND.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Found(response, FOUND) }

            fun SeeOther(response: HttpResponse) =
                SEE_OTHER.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { SeeOther(response, SEE_OTHER) }

            fun NotModified(response: HttpResponse) =
                NOT_MODIFIED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { NotModified(response, NOT_MODIFIED) }

            fun UseProxy(response: HttpResponse) =
                USE_PROXY.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { UseProxy(response, USE_PROXY) }

            fun Unused(response: HttpResponse) =
                REDIRECTION_UNUSED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Unused(response, REDIRECTION_UNUSED) }

            fun TemporaryRedirect(response: HttpResponse) =
                TEMPORARY_REDIRECT.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { TemporaryRedirect(response, TEMPORARY_REDIRECT) }

            fun PermanentRedirect(response: HttpResponse) =
                PERMANENT_REDIRECT.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { PermanentRedirect(response, PERMANENT_REDIRECT) }

            fun Other(response: HttpResponse) =
                REDIRECTION_OTHER.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Other(response, REDIRECTION_OTHER) }
        }

        class MultipleChoices internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<MultipleChoices>) : Redirection() {
            override fun push() = returnTo.push(this)
        }

        class MovedPermanently internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<MovedPermanently>) : Redirection() {
            override fun push() = returnTo.push(this)
        }

        class Found internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Found>) : Redirection() {
            override fun push() = returnTo.push(this)
        }

        class SeeOther internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<SeeOther>) : Redirection() {
            override fun push() = returnTo.push(this)
        }

        class NotModified internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<NotModified>) : Redirection() {
            override fun push() = returnTo.push(this)
        }

        class UseProxy internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<UseProxy>) : Redirection() {
            override fun push() = returnTo.push(this)
        }

        class Unused internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Unused>) : Redirection() {
            override fun push() = returnTo.push(this)
        }

        class TemporaryRedirect internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<TemporaryRedirect>) : Redirection() {
            override fun push() = returnTo.push(this)
        }

        class PermanentRedirect internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<PermanentRedirect>) : Redirection() {
            override fun push() = returnTo.push(this)
        }

        class Other internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Other>) : Redirection() {
            override fun push() = returnTo.push(this)
        }

        override fun get(): Nothing = throw IllegalStateException("Response had status code 3xx - ${_response.status}")

        override fun dataHashCode(): Optional<Int> =
            super<KorneaHttpResult>.dataHashCode()

        override fun isAvailable(dataHashCode: Int?): Boolean? =
            super<KorneaHttpResult>.isAvailable(dataHashCode)

        override fun consume(dataHashCode: Int?) =
            super<KorneaHttpResult>.consume(dataHashCode)
    }

    sealed class ClientError : KorneaHttpResult<Nothing>(), KorneaResult.Failure {
        companion object {
            fun BadRequest(response: HttpResponse) =
                BAD_REQUEST.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { BadRequest(response, BAD_REQUEST) }

            fun Unauthorized(response: HttpResponse) =
                UNAUTHORIZED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Unauthorized(response, UNAUTHORIZED) }

            fun PaymentRequired(response: HttpResponse) =
                PAYMENT_REQUIRED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { PaymentRequired(response, PAYMENT_REQUIRED) }

            fun Forbidden(response: HttpResponse) =
                FORBIDDEN.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Forbidden(response, FORBIDDEN) }

            fun NotFound(response: HttpResponse) =
                NOT_FOUND.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { NotFound(response, NOT_FOUND) }

            fun MethodNotAllowed(response: HttpResponse) =
                METHOD_NOT_ALLOWED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { MethodNotAllowed(response, METHOD_NOT_ALLOWED) }

            fun NotAcceptable(response: HttpResponse) =
                NOT_ACCEPTABLE.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { NotAcceptable(response, NOT_ACCEPTABLE) }

            fun ProxyAuthenticationRequired(response: HttpResponse) =
                PROXY_AUTHENTICATION_REQUIRED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { ProxyAuthenticationRequired(response, PROXY_AUTHENTICATION_REQUIRED) }

            fun RequestTimeout(response: HttpResponse) =
                REQUEST_TIMEOUT.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { RequestTimeout(response, REQUEST_TIMEOUT) }

            fun Conflict(response: HttpResponse) =
                CONFLICT.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Conflict(response, CONFLICT) }

            fun Gone(response: HttpResponse) =
                GONE.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Gone(response, GONE) }

            fun LengthRequired(response: HttpResponse) =
                LENGTH_REQUIRED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { LengthRequired(response, LENGTH_REQUIRED) }

            fun PreconditionFailed(response: HttpResponse) =
                PRECONDITION_FAILED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { PreconditionFailed(response, PRECONDITION_FAILED) }

            fun RequestEntityTooLarge(response: HttpResponse) =
                REQUEST_ENTITY_TOO_LARGE.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { RequestEntityTooLarge(response, REQUEST_ENTITY_TOO_LARGE) }

            fun RequestUriTooLong(response: HttpResponse) =
                REQUEST_URI_TOO_LONG.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { RequestURITooLong(response, REQUEST_URI_TOO_LONG) }

            fun UnsupportedMediaType(response: HttpResponse) =
                UNSUPPORTED_MEDIA_TYPE.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { UnsupportedMediaType(response, UNSUPPORTED_MEDIA_TYPE) }

            fun RequestedRangeNotSatisfiable(response: HttpResponse) =
                REQUESTED_RANGE_NOT_SATISFIABLE.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { RequestedRangeNotSatisfiable(response, REQUESTED_RANGE_NOT_SATISFIABLE) }

            fun ExpectationFailed(response: HttpResponse) =
                EXPECTATION_FAILED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { ExpectationFailed(response, EXPECTATION_FAILED) }

            fun ImATeapot(response: HttpResponse) =
                IM_A_TEAPOT.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { ImATeapot(response, IM_A_TEAPOT) }

            fun EnhanceYourCalm(response: HttpResponse) =
                ENHANCE_YOUR_CALM.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { EnhanceYourCalm(response, ENHANCE_YOUR_CALM) }

            fun UnprocessableEntity(response: HttpResponse) =
                UNPROCESSABLE_ENTITY.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { UnprocessableEntity(response, UNPROCESSABLE_ENTITY) }

            fun Locked(response: HttpResponse) =
                LOCKED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Locked(response, LOCKED) }

            fun FailedDependency(response: HttpResponse) =
                FAILED_DEPENDENCY.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { FailedDependency(response, FAILED_DEPENDENCY) }

            fun ReservedForWebDAV(response: HttpResponse) =
                CLIENT_ERROR_RESERVED_FOR_WEBDAV.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { ReservedForWebDAV(response, CLIENT_ERROR_RESERVED_FOR_WEBDAV) }

            fun UpgradeRequired(response: HttpResponse) =
                UPGRADE_REQUIRED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { UpgradeRequired(response, UPGRADE_REQUIRED) }

            fun PreconditionRequired(response: HttpResponse) =
                PRECONDITION_REQUIRED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { PreconditionRequired(response, PRECONDITION_REQUIRED) }

            fun TooManyRequests(response: HttpResponse) =
                TOO_MANY_REQUESTS.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { TooManyRequests(response, TOO_MANY_REQUESTS) }

            fun RequestHeaderFieldsTooLarge(response: HttpResponse) =
                REQUEST_HEADER_FIELDS_TOO_LARGE.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { RequestHeaderFieldsTooLarge(response, REQUEST_HEADER_FIELDS_TOO_LARGE) }

            fun NoResponse(response: HttpResponse) =
                NO_RESPONSE.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { NoResponse(response, NO_RESPONSE) }

            fun RetryWith(response: HttpResponse) =
                RETRY_WITH.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { RetryWith(response, RETRY_WITH) }

            fun BlockedByWindowsParentalControls(response: HttpResponse) =
                BLOCKED_BY_WINDOWS_PARENTAL_CONTROLS.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { BlockedByWindowsParentalControls(response, BLOCKED_BY_WINDOWS_PARENTAL_CONTROLS) }

            fun UnavailableForLegalReasons(response: HttpResponse) =
                UNAVAILABLE_FOR_LEGAL_REASONS.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { UnavailableForLegalReasons(response, UNAVAILABLE_FOR_LEGAL_REASONS) }

            fun ClientClosedRequest(response: HttpResponse) =
                CLIENT_CLOSED_REQUEST.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { ClientClosedRequest(response, CLIENT_CLOSED_REQUEST) }

            fun Other(response: HttpResponse) =
                CLIENT_ERROR_OTHER.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Other(response, CLIENT_ERROR_OTHER) }
        }

        class BadRequest internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<BadRequest>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class Unauthorized internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Unauthorized>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class PaymentRequired internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<PaymentRequired>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class Forbidden internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Forbidden>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class NotFound internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<NotFound>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class MethodNotAllowed internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<MethodNotAllowed>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class NotAcceptable internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<NotAcceptable>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class ProxyAuthenticationRequired internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<ProxyAuthenticationRequired>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class RequestTimeout internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<RequestTimeout>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class Conflict internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Conflict>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class Gone internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Gone>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class LengthRequired internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<LengthRequired>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class PreconditionFailed internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<PreconditionFailed>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class RequestEntityTooLarge internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<RequestEntityTooLarge>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class RequestURITooLong internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<RequestURITooLong>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class UnsupportedMediaType internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<UnsupportedMediaType>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class RequestedRangeNotSatisfiable internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<RequestedRangeNotSatisfiable>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class ExpectationFailed internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<ExpectationFailed>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class ImATeapot internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<ImATeapot>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class EnhanceYourCalm internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<EnhanceYourCalm>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class UnprocessableEntity internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<UnprocessableEntity>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class Locked internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Locked>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class FailedDependency internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<FailedDependency>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class ReservedForWebDAV internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<ReservedForWebDAV>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class UpgradeRequired internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<UpgradeRequired>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class PreconditionRequired internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<PreconditionRequired>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class TooManyRequests internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<TooManyRequests>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class RequestHeaderFieldsTooLarge internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<RequestHeaderFieldsTooLarge>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class NoResponse internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<NoResponse>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class RetryWith internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<RetryWith>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class BlockedByWindowsParentalControls internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<BlockedByWindowsParentalControls>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class UnavailableForLegalReasons internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<UnavailableForLegalReasons>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class ClientClosedRequest internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<ClientClosedRequest>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        class Other internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Other>) : ClientError() {
            override fun push() = returnTo.push(this)
        }

        override fun get(): Nothing = throw IllegalStateException("Response had status code 4xx - ${_response.status}")

        override fun dataHashCode(): Optional<Int> =
            super<KorneaHttpResult>.dataHashCode()

        override fun isAvailable(dataHashCode: Int?): Boolean? =
            super<KorneaHttpResult>.isAvailable(dataHashCode)

        override fun consume(dataHashCode: Int?) =
            super<KorneaHttpResult>.consume(dataHashCode)
    }

    sealed class ServerError : KorneaHttpResult<Nothing>(), KorneaResult.Failure {
        companion object {
            fun InternalServerError(response: HttpResponse) =
                INTERNAL_SERVER_ERROR.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { InternalServerError(response, INTERNAL_SERVER_ERROR) }

            fun NotImplemented(response: HttpResponse) =
                NOT_IMPLEMENTED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { NotImplemented(response, NOT_IMPLEMENTED) }

            fun BadGateway(response: HttpResponse) =
                BAD_GATEWAY.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { BadGateway(response, BAD_GATEWAY) }

            fun ServiceUnavailable(response: HttpResponse) =
                SERVICE_UNAVAILABLE.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { ServiceUnavailable(response, SERVICE_UNAVAILABLE) }

            fun GatewayTimeout(response: HttpResponse) =
                GATEWAY_TIMEOUT.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { GatewayTimeout(response, GATEWAY_TIMEOUT) }

            fun HTTPVersionNotSupported(response: HttpResponse) =
                HTTP_VERSION_NOT_SUPPORTED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { HTTPVersionNotSupported(response, HTTP_VERSION_NOT_SUPPORTED) }

            fun VariantAlsoNegotiates(response: HttpResponse) =
                VARIANT_ALSO_NEGOTIATES.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { VariantAlsoNegotiates(response, VARIANT_ALSO_NEGOTIATES) }

            fun InsufficientStorage(response: HttpResponse) =
                INSUFFICIENT_STORAGE.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { InsufficientStorage(response, INSUFFICIENT_STORAGE) }

            fun LoopDetected(response: HttpResponse) =
                LOOP_DETECTED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { LoopDetected(response, LOOP_DETECTED) }

            fun BandwidthLimitExceeded(response: HttpResponse) =
                BANDWIDTH_LIMIT_EXCEEDED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { BandwidthLimitExceeded(response, BANDWIDTH_LIMIT_EXCEEDED) }

            fun NotExtended(response: HttpResponse) =
                NOT_EXTENDED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { NotExtended(response, NOT_EXTENDED) }

            fun NetworkAuthenticationRequired(response: HttpResponse) =
                NETWORK_AUTHENTICATION_REQUIRED.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { NetworkAuthenticationRequired(response, NETWORK_AUTHENTICATION_REQUIRED) }

            fun NetworkReadTimeoutError(response: HttpResponse) =
                NETWORK_READ_TIMEOUT.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { NetworkReadTimeoutError(response, NETWORK_READ_TIMEOUT) }

            fun NetworkConnectTimeoutError(response: HttpResponse) =
                NETWORK_CONNECT_TIMEOUT.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { NetworkConnectTimeoutError(response, NETWORK_CONNECT_TIMEOUT) }

            fun Other(response: HttpResponse) =
                SERVER_ERROR_OTHER.pop()
                    .doOnPresent { result -> result._response = response }
                    .getOrElseRun { Other(response, SERVER_ERROR_OTHER) }
        }

        class InternalServerError internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<InternalServerError>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class NotImplemented internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<NotImplemented>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class BadGateway internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<BadGateway>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class ServiceUnavailable internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<ServiceUnavailable>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class GatewayTimeout internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<GatewayTimeout>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class HTTPVersionNotSupported internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<HTTPVersionNotSupported>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class VariantAlsoNegotiates internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<VariantAlsoNegotiates>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class InsufficientStorage internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<InsufficientStorage>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class LoopDetected internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<LoopDetected>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class BandwidthLimitExceeded internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<BandwidthLimitExceeded>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class NotExtended internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<NotExtended>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class NetworkAuthenticationRequired internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<NetworkAuthenticationRequired>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class NetworkReadTimeoutError internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<NetworkReadTimeoutError>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class NetworkConnectTimeoutError internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<NetworkConnectTimeoutError>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        class Other internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Other>) : ServerError() {
            override fun push() = returnTo.push(this)
        }

        override fun get(): Nothing = throw IllegalStateException("Response had status code 5xx - ${_response.status}")

        override fun dataHashCode(): Optional<Int> =
            super<KorneaHttpResult>.dataHashCode()

        override fun isAvailable(dataHashCode: Int?): Boolean? =
            super<KorneaHttpResult>.isAvailable(dataHashCode)

        override fun consume(dataHashCode: Int?) =
            super<KorneaHttpResult>.consume(dataHashCode)
    }

    class Other internal constructor(override var _response: HttpResponse, internal val returnTo: RingBuffer<Other>) : KorneaHttpResult<Nothing>(), KorneaResult.Failure {
        override fun get(): Nothing = throw IllegalStateException("Response had status code ${_response.status}")

        override fun dataHashCode(): Optional<Int> =
            super<KorneaHttpResult>.dataHashCode()

        override fun isAvailable(dataHashCode: Int?): Boolean? =
            super<KorneaHttpResult>.isAvailable(dataHashCode)

        override fun consume(dataHashCode: Int?) =
            super<KorneaHttpResult>.consume(dataHashCode)

        override fun push() = returnTo.push(this)
    }

    override fun dataHashCode(): Optional<Int> =
        if (_response.isComplete() != false) Optional.empty() else Optional.of(_response.hashCode())

    override fun isAvailable(dataHashCode: Int?): Boolean? =
        if (dataHashCode?.equals(_response.hashCode()) != false) _response.isComplete() else null

    override fun consume(dataHashCode: Int?) {
        dataHashCode().doOnPresent { code ->
            if (dataHashCode?.equals(code) != false) {
                latch -= 1
                if (latch == 0) {
                    _response.cleanup()
                    push()
                }
            }
        }
    }

    override fun copyOf(): KorneaResult<T> {
        latch += 1

        return this
    }

    abstract fun push()
}

