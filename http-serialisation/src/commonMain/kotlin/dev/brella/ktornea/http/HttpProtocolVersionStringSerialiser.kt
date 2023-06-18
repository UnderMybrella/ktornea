package dev.brella.ktornea.http

import dev.brella.kornea.serialisation.core.common.AsStringSerializer
import io.ktor.http.*



public object HttpProtocolVersionStringSerialiser : AsStringSerializer<HttpProtocolVersion>(
    "HttpProtocolVersion",
    HttpProtocolVersion::toString,
    HttpProtocolVersion.Companion::parse
)
