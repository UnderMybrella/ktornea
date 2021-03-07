package dev.brella.ktornea.common

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

suspend inline fun HttpClient.makeRequest(builder: HttpRequestBuilder.() -> Unit): HttpResponse =
    request(builder)