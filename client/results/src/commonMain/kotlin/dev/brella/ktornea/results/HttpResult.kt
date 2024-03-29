package dev.brella.ktornea.results

import dev.brella.kornea.errors.common.KorneaResult
import io.ktor.client.statement.*

public interface HttpResult : KorneaResult.Failure {
    public val response: HttpResponse

    public interface Informational : HttpResult, KorneaResult.Empty {
        public companion object {
            internal fun <T> of(response: HttpResponse, cause: KorneaResult.Failure? = null): KorneaResult<T> =
                KorneaResult.failure(Base(response, cause))

            internal fun <T, R> of(
                response: HttpResponse,
                body: R,
                cause: KorneaResult.Failure? = null,
            ): KorneaResult<T> =
                KorneaResult.failure(BaseWithBody(response, body, cause))
        }

        private class Base(override val response: HttpResponse, override val cause: KorneaResult.Failure?) :
            Informational {
            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload)

            override fun withCause(newCause: KorneaResult.Failure?): KorneaResult.Failure =
                Base(response, newCause)

            override fun <R> with(newPayload: R, newCause: KorneaResult.Failure?): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, newCause)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 1xx - ${response.status}")
        }

        private class BaseWithBody<out T>(
            override val response: HttpResponse,
            override val payload: T,
            override val cause: KorneaResult.Failure? = null,
        ) : Informational, KorneaResult.WithPayload<T> {
            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload)

            override fun withCause(newCause: KorneaResult.Failure?): KorneaResult.WithPayload<T> =
                BaseWithBody(response, payload, newCause)

            override fun <R> with(newPayload: R, newCause: KorneaResult.Failure?): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, newCause)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 1xx - ${response.status} ($payload)")
        }
    }

    public interface Success : HttpResult, KorneaResult.Empty {
        public companion object {
            internal fun <T> of(response: HttpResponse, cause: KorneaResult.Failure? = null): KorneaResult<T> =
                KorneaResult.failure(Base(response, cause))

            internal fun <T, R> of(
                response: HttpResponse,
                body: R,
                cause: KorneaResult.Failure? = null,
            ): KorneaResult<T> =
                KorneaResult.failure(BaseWithBody(response, body, cause))
        }

        private class Base(override val response: HttpResponse, override val cause: KorneaResult.Failure?) : Success {

            override fun withCause(newCause: KorneaResult.Failure?): KorneaResult.Failure =
                Base(response, newCause)

            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, cause)

            override fun <R> with(newPayload: R, newCause: KorneaResult.Failure?): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, newCause)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 2xx - ${response.status}")
        }

        private class BaseWithBody<out T>(
            override val response: HttpResponse,
            override val payload: T,
            override val cause: KorneaResult.Failure?,
        ) : Success, KorneaResult.WithPayload<T> {

            override fun withCause(newCause: KorneaResult.Failure?): KorneaResult.WithPayload<T> =
                BaseWithBody(response, payload, newCause)

            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, cause)

            override fun <R> with(newPayload: R, newCause: KorneaResult.Failure?): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, newCause)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 2xx - ${response.status} ($payload)")
        }
    }

    public interface Redirection : HttpResult, KorneaResult.Empty {
        public companion object {
            internal fun <T> of(response: HttpResponse, cause: KorneaResult.Failure? = null): KorneaResult<T> =
                KorneaResult.failure(Base(response, cause))

            internal fun <T, R> of(
                response: HttpResponse,
                body: R,
                cause: KorneaResult.Failure? = null,
            ): KorneaResult<T> =
                KorneaResult.failure(BaseWithBody(response, body, cause))
        }

        private class Base(
            override val response: HttpResponse,
            override val cause: KorneaResult.Failure?,
        ) : Redirection {

            override fun withCause(newCause: KorneaResult.Failure?): KorneaResult.Failure =
                Base(response, newCause)

            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, cause)

            override fun <R> with(newPayload: R, newCause: KorneaResult.Failure?): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, newCause)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 3xx - ${response.status}")
        }

        private class BaseWithBody<out T>(
            override val response: HttpResponse,
            override val payload: T,
            override val cause: KorneaResult.Failure?,
        ) : Redirection, KorneaResult.WithPayload<T> {

            override fun withCause(newCause: KorneaResult.Failure?): KorneaResult.WithPayload<T> =
                BaseWithBody(response, payload, newCause)

            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, cause)

            override fun <R> with(newPayload: R, newCause: KorneaResult.Failure?): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, newCause)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 3xx - ${response.status} ($payload)")
        }
    }

    public interface ClientError : HttpResult {
        public companion object {
            internal fun <T> of(response: HttpResponse, cause: KorneaResult.Failure? = null): KorneaResult<T> =
                KorneaResult.failure(Base(response, cause))

            internal fun <T, R> of(
                response: HttpResponse,
                body: R,
                cause: KorneaResult.Failure? = null,
            ): KorneaResult<T> =
                KorneaResult.failure(BaseWithBody(response, body, cause))
        }

        private class Base(override val response: HttpResponse, override val cause: KorneaResult.Failure?) :
            ClientError {
            override fun withCause(newCause: KorneaResult.Failure?): KorneaResult.Failure =
                Base(response, newCause)

            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, cause)

            override fun <R> with(newPayload: R, newCause: KorneaResult.Failure?): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, newCause)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 4xx - ${response.status}")
        }

        private class BaseWithBody<out T>(
            override val response: HttpResponse,
            override val payload: T,
            override val cause: KorneaResult.Failure?,
        ) : ClientError, KorneaResult.WithPayload<T> {

            override fun withCause(newCause: KorneaResult.Failure?): KorneaResult.WithPayload<T> =
                BaseWithBody(response, payload, newCause)

            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, cause)

            override fun <R> with(newPayload: R, newCause: KorneaResult.Failure?): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, newCause)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 4xx - ${response.status} ($payload)")
        }
    }

    public interface ServerError : HttpResult {
        public companion object {
            internal fun <T> of(response: HttpResponse, cause: KorneaResult.Failure? = null): KorneaResult<T> =
                KorneaResult.failure(Base(response, cause))

            internal fun <T, R> of(
                response: HttpResponse,
                body: R,
                cause: KorneaResult.Failure? = null,
            ): KorneaResult<T> =
                KorneaResult.failure(BaseWithBody(response, body, cause))
        }

        private class Base(
            override val response: HttpResponse,
            override val cause: KorneaResult.Failure?,
        ) : ServerError {

            override fun withCause(newCause: KorneaResult.Failure?): KorneaResult.Failure =
                Base(response, newCause)

            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, cause)

            override fun <R> with(newPayload: R, newCause: KorneaResult.Failure?): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, newCause)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 5xx - ${response.status}")
        }

        private class BaseWithBody<out T>(
            override val response: HttpResponse,
            override val payload: T,
            override val cause: KorneaResult.Failure?,
        ) : ServerError, KorneaResult.WithPayload<T> {

            override fun withCause(newCause: KorneaResult.Failure?): KorneaResult.WithPayload<T> =
                BaseWithBody(response, payload, newCause)

            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, cause)

            override fun <R> with(newPayload: R, newCause: KorneaResult.Failure?): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload, newCause)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 5xx - ${response.status} ($payload)")
        }
    }

    private class Base(override val response: HttpResponse, override val cause: KorneaResult.Failure?) : HttpResult {

        override fun withCause(newCause: KorneaResult.Failure?): KorneaResult.Failure =
            Base(response, newCause)

        override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
            BaseWithBody(response, newPayload, cause)

        override fun <R> with(newPayload: R, newCause: KorneaResult.Failure?): KorneaResult.WithPayload<R> =
            BaseWithBody(response, newPayload, newCause)

        override fun asException(): Throwable =
            IllegalArgumentException("Response has unknown status code - ${response.status}")
    }

    private class BaseWithBody<out T>(
        override val response: HttpResponse,
        override val payload: T,
        override val cause: KorneaResult.Failure?,
    ) : HttpResult, KorneaResult.WithPayload<T> {

        override fun withCause(newCause: KorneaResult.Failure?): KorneaResult.WithPayload<T> =
            BaseWithBody(response, payload, newCause)

        override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
            BaseWithBody(response, newPayload, cause)

        override fun <R> with(newPayload: R, newCause: KorneaResult.Failure?): KorneaResult.WithPayload<R> =
            BaseWithBody(response, newPayload, newCause)

        override fun asException(): Throwable =
            IllegalArgumentException("Response has unknown status code - ${response.status} ($payload)")
    }

    public companion object {
        private val INFORMATIONAL_RANGE = 100..199
        private val SUCCESS_RANGE = 200..299
        private val REDIRECTION_RANGE = 300..399
        private val CLIENT_ERROR_RANGE = 400..499
        private val SERVER_ERROR_RANGE = 500..599

        public fun <T> of(response: HttpResponse, cause: KorneaResult.Failure? = null): KorneaResult<T> =
            when (response.status.value) {
                in INFORMATIONAL_RANGE -> Informational.of(response, cause)
                in SUCCESS_RANGE -> Success.of(response, cause)
                in REDIRECTION_RANGE -> Redirection.of(response, cause)
                in CLIENT_ERROR_RANGE -> ClientError.of(response, cause)
                in SERVER_ERROR_RANGE -> ServerError.of(response, cause)
                else -> KorneaResult.failure(Base(response, cause))
            }

        public fun <T> of(response: HttpResponse, payload: Any?, cause: KorneaResult.Failure? = null): KorneaResult<T> =
            when (response.status.value) {
                in INFORMATIONAL_RANGE -> Informational.of(response, payload, cause)
                in SUCCESS_RANGE -> Success.of(response, payload, cause)
                in REDIRECTION_RANGE -> Redirection.of(response, payload, cause)
                in CLIENT_ERROR_RANGE -> ClientError.of(response, payload, cause)
                in SERVER_ERROR_RANGE -> ServerError.of(response, payload, cause)
                else -> KorneaResult.failure(BaseWithBody(response, payload, cause))
            }
    }
}