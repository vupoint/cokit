package io.github.vupoint.cokit.client

import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/** A field whose omission differs from an explicitly supplied null. */
@Serializable(with = CodexOptionalSerializer::class)
sealed interface CodexOptional<out T : Any> {
    data object Omitted : CodexOptional<Nothing>
    data class Value<T : Any>(val value: T?) : CodexOptional<T>
}

class CodexOptionalSerializer<T : Any>(private val serializer: KSerializer<T>) : KSerializer<CodexOptional<T>> {
    override val descriptor = serializer.nullable.descriptor

    override fun serialize(encoder: Encoder, value: CodexOptional<T>) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("CodexOptional requires JSON")
        val present = value as? CodexOptional.Value<T>
            ?: throw SerializationException("Omitted fields must use EncodeDefault.NEVER")
        jsonEncoder.encodeJsonElement(present.value?.let { jsonEncoder.json.encodeToJsonElement(serializer, it) } ?: JsonNull)
    }

    override fun deserialize(decoder: Decoder): CodexOptional<T> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("CodexOptional requires JSON")
        val value = jsonDecoder.decodeJsonElement()
        return CodexOptional.Value(if (value == JsonNull) null else jsonDecoder.json.decodeFromJsonElement(serializer, value))
    }
}
