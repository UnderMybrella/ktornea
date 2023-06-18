package dev.brella.ktornea.http

import dev.brella.kornea.serialisation.core.common.AsStringSerializer
import io.ktor.util.*
import kotlinx.serialization.Serializable

public typealias Base64Array = @Serializable(Base64ArraySerializer::class) ByteArray
public typealias Base64List = @Serializable(Base64ListSerializer::class) List<Byte>

public object Base64ArraySerializer :
    AsStringSerializer<ByteArray>("Base64Array", ByteArray::encodeBase64, String::decodeBase64Bytes)

public object Base64ListSerializer :
    AsStringSerializer<List<Byte>>(
        "Base64List",
        { list -> list.toByteArray().encodeBase64() },
        { str -> str.decodeBase64Bytes().asList() })