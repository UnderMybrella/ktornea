package dev.brella.ktornea.results

import dev.brella.kornea.errors.common.KorneaResult
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

public inline fun wrapAndCatchResponse(response: () -> HttpResponse): KorneaResult<HttpResponse> =
    try { wrapInResult(response()) } catch (th: Throwable) { KorneaResult.thrown(th) }

public inline fun wrapInResult(response: HttpResponse): KorneaResult<HttpResponse> =
    if (response.status.isSuccess()) KorneaResult.success(response)
    else HttpResult.of(response)

public suspend inline fun HttpClient.requestResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { request(builder) }

public suspend inline fun HttpClient.getResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { get(builder) }

public suspend inline fun HttpClient.postResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { post(builder) }

public suspend inline fun HttpClient.putResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { put(builder) }

public suspend inline fun HttpClient.deleteResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { delete(builder) }

public suspend inline fun HttpClient.optionsResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { options(builder) }

public suspend inline fun HttpClient.patchResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { patch(builder) }

public suspend inline fun HttpClient.headResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { patch(builder) }



public suspend inline fun HttpClient.getResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { get(urlString, builder) }

public suspend inline fun HttpClient.postResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { post(urlString, builder) }

public suspend inline fun HttpClient.putResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { put(urlString, builder) }

public suspend inline fun HttpClient.deleteResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { delete(urlString, builder) }

public suspend inline fun HttpClient.optionsResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { options(urlString, builder) }

public suspend inline fun HttpClient.patchResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { patch(urlString, builder) }

public suspend inline fun HttpClient.headResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { patch(urlString, builder) }



public suspend inline fun HttpClient.getResult(urlString: String): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { get(urlString) }

public suspend inline fun HttpClient.postResult(urlString: String): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { post(urlString) }

public suspend inline fun HttpClient.putResult(urlString: String): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { put(urlString) }

public suspend inline fun HttpClient.deleteResult(urlString: String): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { delete(urlString) }

public suspend inline fun HttpClient.optionsResult(urlString: String): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { options(urlString) }

public suspend inline fun HttpClient.patchResult(urlString: String): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { patch(urlString) }

public suspend inline fun HttpClient.headResult(urlString: String): KorneaResult<HttpResponse> =
    wrapAndCatchResponse { patch(urlString) }