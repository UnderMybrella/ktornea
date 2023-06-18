package dev.brella.ktornea.http

import io.ktor.http.*
import io.ktor.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private val STRING_MAP_SERIALIZER = MapSerializer(String.serializer(), ListSerializer(String.serializer()))

public object StringValuesSerialiser : KSerializer<StringValues> {
    override val descriptor: SerialDescriptor = STRING_MAP_SERIALIZER.descriptor

    override fun deserialize(decoder: Decoder): StringValues =
        valuesOf(STRING_MAP_SERIALIZER.deserialize(decoder))

    override fun serialize(encoder: Encoder, value: StringValues) {
        STRING_MAP_SERIALIZER.serialize(encoder, value.toMap())
    }

}

public object HeadersSerialiser : KSerializer<Headers> {
    override val descriptor: SerialDescriptor = STRING_MAP_SERIALIZER.descriptor

    override fun deserialize(decoder: Decoder): Headers =
        HeadersImpl(STRING_MAP_SERIALIZER.deserialize(decoder))

    override fun serialize(encoder: Encoder, value: Headers) {
        value.get(HttpHeaders.Accept)
        STRING_MAP_SERIALIZER.serialize(encoder, value.toMap())
    }
}