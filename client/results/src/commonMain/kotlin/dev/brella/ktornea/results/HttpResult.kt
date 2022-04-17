package dev.brella.ktornea.results

import dev.brella.kornea.errors.common.KorneaResult
import io.ktor.client.statement.*

public interface HttpResult : KorneaResult.Failure {
    public val response: HttpResponse

    public interface Informational : HttpResult, KorneaResult.Empty {
        public companion object {
            internal fun <T> of(response: HttpResponse): KorneaResult<T> =
                KorneaResult.failure(Base(response))
        }

        private class Base(override val response: HttpResponse) : Informational {
            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 1xx - ${response.status}")
        }
    }

    public interface Success : HttpResult, KorneaResult.Empty {
        public companion object {
            internal fun <T> of(response: HttpResponse): KorneaResult<T> =
                KorneaResult.failure(Base(response))
        }

        private class Base(override val response: HttpResponse) : Success {
            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 2xx - ${response.status}")
        }
    }

    public interface Redirection : HttpResult, KorneaResult.Empty {
        public companion object {
            internal fun <T> of(response: HttpResponse): KorneaResult<T> =
                KorneaResult.failure(Base(response))
        }

        private class Base(override val response: HttpResponse) : Redirection {
            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 3xx - ${response.status}")
        }
    }

    public interface ClientError : HttpResult {
        public companion object {
            internal fun <T> of(response: HttpResponse): KorneaResult<T> =
                KorneaResult.failure(Base(response))
        }

        private class Base(override val response: HttpResponse) : ClientError {
            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 4xx - ${response.status}")
        }
    }

    public interface ServerError : HttpResult {
        public companion object {
            internal fun <T> of(response: HttpResponse): KorneaResult<T> =
                KorneaResult.failure(Base(response))
        }

        private class Base(override val response: HttpResponse) : ServerError {
            override fun asException(): Throwable =
                IllegalArgumentException("Response has status code 5xx - ${response.status}")
        }
    }

    private class Base(override val response: HttpResponse) : HttpResult {
        override fun asException(): Throwable =
            IllegalArgumentException("Response has status code 1xx - ${response.status}")
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
    }
}