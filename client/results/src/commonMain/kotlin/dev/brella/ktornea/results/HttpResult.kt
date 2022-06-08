package dev.brella.ktornea.results

import dev.brella.kornea.errors.common.KorneaResult
import io.ktor.client.statement.*

public interface HttpResult : KorneaResult.Failure {
    public val response: HttpResponse

    public interface Informational : HttpResult, KorneaResult.Empty {
        public companion object {
            internal fun <T> of(response: HttpResponse): KorneaResult<T> =
                KorneaResult.failure(Base(response))

            internal fun <T, R> of(response: HttpResponse, body: R): KorneaResult<T> =
                KorneaResult.failure(BaseWithBody(response, body))
        }

        private class Base(override val response: HttpResponse) : Informational {
            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 1xx - ${response.status}")
        }

        private class BaseWithBody<out T>(override val response: HttpResponse, override val payload: T) : Informational, KorneaResult.WithPayload<T> {
            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 1xx - ${response.status} ($payload)")
        }
    }

    public interface Success : HttpResult, KorneaResult.Empty {
        public companion object {
            internal fun <T> of(response: HttpResponse): KorneaResult<T> =
                KorneaResult.failure(Base(response))

            internal fun <T, R> of(response: HttpResponse, body: R): KorneaResult<T> =
                KorneaResult.failure(BaseWithBody(response, body))
        }

        private class Base(override val response: HttpResponse) : Success {
            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 2xx - ${response.status}")
        }

        private class BaseWithBody<out T>(override val response: HttpResponse, override val payload: T) : Success, KorneaResult.WithPayload<T> {
            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 2xx - ${response.status} ($payload)")
        }
    }

    public interface Redirection : HttpResult, KorneaResult.Empty {
        public companion object {
            internal fun <T> of(response: HttpResponse): KorneaResult<T> =
                KorneaResult.failure(Base(response))

            internal fun <T, R> of(response: HttpResponse, body: R): KorneaResult<T> =
                KorneaResult.failure(BaseWithBody(response, body))
        }

        private class Base(override val response: HttpResponse) : Redirection {
            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 3xx - ${response.status}")
        }

        private class BaseWithBody<out T>(override val response: HttpResponse, override val payload: T) : Redirection, KorneaResult.WithPayload<T> {
            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 3xx - ${response.status} ($payload)")
        }
    }

    public interface ClientError : HttpResult {
        public companion object {
            internal fun <T> of(response: HttpResponse): KorneaResult<T> =
                KorneaResult.failure(Base(response))

            internal fun <T, R> of(response: HttpResponse, body: R): KorneaResult<T> =
                KorneaResult.failure(BaseWithBody(response, body))
        }

        private class Base(override val response: HttpResponse) : ClientError {
            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 4xx - ${response.status}")
        }

        private class BaseWithBody<out T>(override val response: HttpResponse, override val payload: T) : ClientError, KorneaResult.WithPayload<T> {
            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 4xx - ${response.status} ($payload)")
        }
    }

    public interface ServerError : HttpResult {
        public companion object {
            internal fun <T> of(response: HttpResponse): KorneaResult<T> =
                KorneaResult.failure(Base(response))

            internal fun <T, R> of(response: HttpResponse, body: R): KorneaResult<T> =
                KorneaResult.failure(BaseWithBody(response, body))
        }

        private class Base(override val response: HttpResponse) : ServerError {
            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 5xx - ${response.status}")
        }

        private class BaseWithBody<out T>(override val response: HttpResponse, override val payload: T) : ServerError, KorneaResult.WithPayload<T> {
            override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
                BaseWithBody(response, newPayload)

            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 5xx - ${response.status} ($payload)")
        }
    }

    private class Base(override val response: HttpResponse) : HttpResult {
        override fun asException(): Throwable =
            IllegalArgumentException("Response has unknown status code - ${response.status}")
    }

    private class BaseWithBody<out T>(override val response: HttpResponse, override val payload: T) : HttpResult, KorneaResult.WithPayload<T> {
        override fun <R> withPayload(newPayload: R): KorneaResult.WithPayload<R> =
            BaseWithBody(response, newPayload)

        override fun asException(): Throwable =
            IllegalArgumentException("Response has unknown status code - ${response.status} ($payload)")
    }

    public companion object {
        private val INFORMATIONAL_RANGE = 100..199
        private val SUCCESS_RANGE = 200..299
        private val REDIRECTION_RANGE = 300..399
        private val CLIENT_ERROR_RANGE = 400..499
        private val SERVER_ERROR_RANGE = 500..599

        public fun <T> of(response: HttpResponse): KorneaResult<T> =
            when (response.status.value) {
                in INFORMATIONAL_RANGE -> Informational.of(response)
                in SUCCESS_RANGE -> Success.of(response)
                in REDIRECTION_RANGE -> Redirection.of(response)
                in CLIENT_ERROR_RANGE -> ClientError.of(response)
                in SERVER_ERROR_RANGE -> ServerError.of(response)
                else -> KorneaResult.failure(Base(response))
            }

        public fun <T> of(response: HttpResponse, payload: Any?): KorneaResult<T> =
            when (response.status.value) {
                in INFORMATIONAL_RANGE -> Informational.of(response, payload)
                in SUCCESS_RANGE -> Success.of(response, payload)
                in REDIRECTION_RANGE -> Redirection.of(response, payload)
                in CLIENT_ERROR_RANGE -> ClientError.of(response, payload)
                in SERVER_ERROR_RANGE -> ServerError.of(response, payload)
                else -> KorneaResult.failure(BaseWithBody(response, payload))
            }
    }
}