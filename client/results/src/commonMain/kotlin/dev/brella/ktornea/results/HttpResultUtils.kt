package dev.brella.ktornea.results

import dev.brella.kornea.errors.common.KorneaResult
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

public inline fun wrapInResult(response: HttpResponse): KorneaResult<HttpResponse> =
    if (response.status.isSuccess()) KorneaResult.success(response)
    else HttpResult.of(response)

public suspend inline fun HttpClient.requestResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(request(builder))

public suspend inline fun HttpClient.getResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(get(builder))

public suspend inline fun HttpClient.postResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(post(builder))

public suspend inline fun HttpClient.putResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(put(builder))

public suspend inline fun HttpClient.deleteResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(delete(builder))

public suspend inline fun HttpClient.optionsResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(options(builder))

public suspend inline fun HttpClient.patchResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(patch(builder))

public suspend inline fun HttpClient.headResult(builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(patch(builder))



public suspend inline fun HttpClient.getResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(get(urlString, builder))

public suspend inline fun HttpClient.postResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(post(urlString, builder))

public suspend inline fun HttpClient.putResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(put(urlString, builder))

public suspend inline fun HttpClient.deleteResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(delete(urlString, builder))

public suspend inline fun HttpClient.optionsResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(options(urlString, builder))

public suspend inline fun HttpClient.patchResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(patch(urlString, builder))

public suspend inline fun HttpClient.headResult(urlString: String, builder: HttpRequestBuilder.() -> Unit): KorneaResult<HttpResponse> =
    wrapInResult(patch(urlString, builder))