package dev.brella.ktornea.http

import dev.brella.kornea.serialisation.core.common.KorneaNumericalEnumLikeSerialiser
import io.ktor.http.*

public object HttpStatusCodeIntSerialiser : KorneaNumericalEnumLikeSerialiser.MapBased.AsInt<HttpStatusCode>(
    ExtendedHttpStatusCode.statusCodesMap,
    "dev.brella.ktornea.http.HttpStatusCodeIntSerialiser",
    { HttpStatusCode(it, "(Unknown)") },
    HttpStatusCode::value
)