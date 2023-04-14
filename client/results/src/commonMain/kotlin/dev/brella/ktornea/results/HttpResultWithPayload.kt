package dev.brella.ktornea.results

import dev.brella.kornea.errors.common.KorneaErrors
import dev.brella.kornea.errors.common.KorneaResult
import dev.brella.kornea.errors.common.withPayloadForTypedFailure
import io.ktor.http.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public inline fun <T> KorneaResult<T>.withPayloadForHttpResult(block: (HttpResult) -> Any?): KorneaResult<T> =
    withPayloadForTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <T> KorneaResult<T>.withPayloadForHttpResult(
    statusCode: HttpStatusCode,
    block: (HttpResult) -> Any?,
): KorneaResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return when (val failure = failureOrNull()) {
        is HttpResult ->
            if (failure.response.status == statusCode) KorneaResult.failure(failure withPayload block(failure))
            else this

        else -> this
    }
}

public inline fun <T> KorneaResult<T>.withPayloadForHttpInformational(block: (HttpResult.Informational) -> KorneaResult.Failure): KorneaResult<T> =
    withPayloadForTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <T> KorneaResult<T>.withPayloadForHttpInformational(
    statusCode: HttpStatusCode,
    block: (HttpResult.Informational) -> Any?,
): KorneaResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return when (val failure = failureOrNull()) {
        is HttpResult.Informational ->
            if (failure.response.status == statusCode) KorneaResult.failure(failure withPayload block(failure))
            else this

        else -> this
    }
}

public inline fun <T> KorneaResult<T>.withPayloadForHttpSuccess(block: (HttpResult.Success) -> KorneaResult.Failure): KorneaResult<T> =
    withPayloadForTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <T> KorneaResult<T>.withPayloadForHttpSuccess(
    statusCode: HttpStatusCode,
    block: (HttpResult.Success) -> Any?,
): KorneaResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return when (val failure = failureOrNull()) {
        is HttpResult.Success ->
            if (failure.response.status == statusCode) KorneaResult.failure(failure withPayload block(failure))
            else this

        else -> this
    }
}

public inline fun <T> KorneaResult<T>.withPayloadForHttpRedirection(block: (HttpResult.Redirection) -> KorneaResult.Failure): KorneaResult<T> =
    withPayloadForTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <T> KorneaResult<T>.withPayloadForHttpRedirection(
    statusCode: HttpStatusCode,
    block: (HttpResult.Redirection) -> Any?,
): KorneaResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return when (val failure = failureOrNull()) {
        is HttpResult.Redirection ->
            if (failure.response.status == statusCode) KorneaResult.failure(failure withPayload block(failure))
            else this

        else -> this
    }
}

public inline fun <T> KorneaResult<T>.withPayloadForHttpClientError(block: (HttpResult.ClientError) -> KorneaResult.Failure): KorneaResult<T> =
    withPayloadForTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <T> KorneaResult<T>.withPayloadForHttpClientError(
    statusCode: HttpStatusCode,
    block: (HttpResult.ClientError) -> Any?,
): KorneaResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return when (val failure = failureOrNull()) {
        is HttpResult.ClientError ->
            if (failure.response.status == statusCode) KorneaResult.failure(failure withPayload block(failure))
            else this

        else -> this
    }
}

public inline fun <T> KorneaResult<T>.withPayloadForHttpServerError(block: (HttpResult.ServerError) -> KorneaResult.Failure): KorneaResult<T> =
    withPayloadForTypedFailure(block)

@OptIn(ExperimentalContracts::class)
public inline fun <T> KorneaResult<T>.withPayloadForHttpServerError(
    statusCode: HttpStatusCode,
    block: (HttpResult.ServerError) -> Any?,
): KorneaResult<T> {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }

    return when (val failure = failureOrNull()) {
        is HttpResult.ServerError ->
            if (failure.response.status == statusCode) KorneaResult.failure(failure withPayload block(failure))
            else this

        else -> this
    }
}