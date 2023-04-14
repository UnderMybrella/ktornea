package dev.brella.ktornea.http

import io.ktor.http.*
import kotlinx.serialization.Serializable

public typealias HttpStatusCodeFromInt = @Serializable(HttpStatusCodeIntSerialiser::class) HttpStatusCode