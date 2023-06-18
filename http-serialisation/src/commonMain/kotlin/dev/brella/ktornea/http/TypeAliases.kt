package dev.brella.ktornea.http

import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.Serializable

public typealias HttpIntStatusCode = @Serializable(HttpStatusCodeIntSerialiser::class) HttpStatusCode
public typealias HttpStringProtocolVersion = @Serializable(HttpProtocolVersionStringSerialiser::class) HttpProtocolVersion
public typealias SerializableHeaders = @Serializable(HeadersSerialiser::class) Headers
public typealias SerializableStringValues = @Serializable(StringValuesSerialiser::class) StringValues