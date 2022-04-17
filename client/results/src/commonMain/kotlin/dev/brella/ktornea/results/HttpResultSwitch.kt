package dev.brella.ktornea.results

import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.switchIfTypedFailure
import io.ktor.http.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public inline fun <R, T : R> KorneaResult<T>.switchIfHttpResult(block: (HttpResult) -> KorneaResult<R>): KorneaResult<R> =
    switchIfTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <R, T : R> KorneaResult<T>.switchIfHttpResult(
    statusCode: HttpStatusCode,
    block: (HttpResult) -> KorneaResult<R>
): KorneaResult<R> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return when (val failure = failureOrNull()) {
        is HttpResult -> if (failure.response.status == statusCode) block(failure) else this
        else -> this
    }
}

public inline fun <R, T : R> KorneaResult<T>.switchIfHttpInformational(block: (HttpResult.Informational) -> KorneaResult<R>): KorneaResult<R> =
    switchIfTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <R, T : R> KorneaResult<T>.switchIfHttpInformational(
    statusCode: HttpStatusCode,
    block: (HttpResult.Informational) -> KorneaResult<R>
): KorneaResult<R> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return when (val failure = failureOrNull()) {
        is HttpResult.Informational -> if (failure.response.status == statusCode) block(failure) else this
        else -> this
    }
}

public inline fun <R, T : R> KorneaResult<T>.switchIfHttpSuccess(block: (HttpResult.Success) -> KorneaResult<R>): KorneaResult<R> =
    switchIfTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <R, T : R> KorneaResult<T>.switchIfHttpSuccess(
    statusCode: HttpStatusCode,
    block: (HttpResult.Success) -> KorneaResult<R>
): KorneaResult<R> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return when (val failure = failureOrNull()) {
        is HttpResult.Success -> if (failure.response.status == statusCode) block(failure) else this
        else -> this
    }
}

public inline fun <R, T : R> KorneaResult<T>.switchIfHttpRedirection(block: (HttpResult.Redirection) -> KorneaResult<R>): KorneaResult<R> =
    switchIfTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <R, T : R> KorneaResult<T>.switchIfHttpRedirection(
    statusCode: HttpStatusCode,
    block: (HttpResult.Redirection) -> KorneaResult<R>
): KorneaResult<R> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return when (val failure = failureOrNull()) {
        is HttpResult.Redirection -> if (failure.response.status == statusCode) block(failure) else this
        else -> this
    }
}

public inline fun <R, T : R> KorneaResult<T>.switchIfHttpClientError(block: (HttpResult.ClientError) -> KorneaResult<R>): KorneaResult<R> =
    switchIfTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <R, T : R> KorneaResult<T>.switchIfHttpClientError(
    statusCode: HttpStatusCode,
    block: (HttpResult.ClientError) -> KorneaResult<R>
): KorneaResult<R> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return when (val failure = failureOrNull()) {
        is HttpResult.ClientError -> if (failure.response.status == statusCode) block(failure) else this
        else -> this
    }
}

public inline fun <R, T : R> KorneaResult<T>.switchIfHttpServerError(block: (HttpResult.ServerError) -> KorneaResult<R>): KorneaResult<R> =
    switchIfTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <R, T : R> KorneaResult<T>.switchIfHttpServerError(
    statusCode: HttpStatusCode,
    block: (HttpResult.ServerError) -> KorneaResult<R>
): KorneaResult<R> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return when (val failure = failureOrNull()) {
        is HttpResult.ServerError -> if (failure.response.status == statusCode) block(failure) else this
        else -> this
    }
}