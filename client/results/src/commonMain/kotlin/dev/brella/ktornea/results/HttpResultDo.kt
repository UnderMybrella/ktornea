package dev.brella.ktornea.results

import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.doOnTypedFailure
import io.ktor.http.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public inline fun <T> KorneaResult<T>.doOnHttpResult(block: (HttpResult) -> Unit): KorneaResult<T> =
    doOnTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <T> KorneaResult<T>.doOnHttpResult(statusCode: HttpStatusCode, block: (HttpResult) -> Unit): KorneaResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    when (val failure = failureOrNull()) {
        is HttpResult -> if (failure.response.status == statusCode) block(failure)
    }
    return this
}

public inline fun <T> KorneaResult<T>.doOnHttpInformational(block: (HttpResult.Informational) -> Unit): KorneaResult<T> =
    doOnTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <T> KorneaResult<T>.doOnHttpInformational(statusCode: HttpStatusCode, block: (HttpResult.Informational) -> Unit): KorneaResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    when (val failure = failureOrNull()) {
        is HttpResult.Informational -> if (failure.response.status == statusCode) block(failure)
    }
    return this
}

public inline fun <T> KorneaResult<T>.doOnHttpSuccess(block: (HttpResult.Success) -> Unit): KorneaResult<T> =
    doOnTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <T> KorneaResult<T>.doOnHttpSuccess(statusCode: HttpStatusCode, block: (HttpResult.Success) -> Unit): KorneaResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    when (val failure = failureOrNull()) {
        is HttpResult.Success -> if (failure.response.status == statusCode) block(failure)
    }
    return this
}

public inline fun <T> KorneaResult<T>.doOnHttpRedirection(block: (HttpResult.Redirection) -> Unit): KorneaResult<T> =
    doOnTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <T> KorneaResult<T>.doOnHttpRedirection(statusCode: HttpStatusCode, block: (HttpResult.Redirection) -> Unit): KorneaResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    when (val failure = failureOrNull()) {
        is HttpResult.Redirection -> if (failure.response.status == statusCode) block(failure)
    }
    return this
}

public inline fun <T> KorneaResult<T>.doOnHttpClientError(block: (HttpResult.ClientError) -> Unit): KorneaResult<T> =
    doOnTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <T> KorneaResult<T>.doOnHttpClientError(statusCode: HttpStatusCode, block: (HttpResult.ClientError) -> Unit): KorneaResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    when (val failure = failureOrNull()) {
        is HttpResult.ClientError -> if (failure.response.status == statusCode) block(failure)
    }
    return this
}

public inline fun <T> KorneaResult<T>.doOnHttpServerError(block: (HttpResult.ServerError) -> Unit): KorneaResult<T> =
    doOnTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <T> KorneaResult<T>.doOnHttpServerError(statusCode: HttpStatusCode, block: (HttpResult.ServerError) -> Unit): KorneaResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    when (val failure = failureOrNull()) {
        is HttpResult.ServerError -> if (failure.response.status == statusCode) block(failure)
    }
    return this
}