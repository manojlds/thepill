package com.stacktoheap.thepill.utils

import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.getContextualOrDefault

object JsonUtils {
    @ImplicitReflectionSerializer
    fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        else -> {
            val jsonParser = Json(JsonConfiguration.Stable)
            val serializer = jsonParser.context.getContextualOrDefault(this)
            jsonParser.toJson(serializer, this)
        }
    }

    fun JsonElement.toPrimitive(): Any? = when (this) {
        is JsonNull -> null
        is JsonLiteral -> {
            if (isString) {
                contentOrNull
            } else {
                booleanOrNull ?: longOrNull ?: doubleOrNull
            }
        }
        else -> null
    }

    fun JsonObject.toPrimitiveMap(): Map<String, Any> =
        this.content.map {
            it.key to it.value.toPrimitive()!!
        }.toMap()

    @ImplicitReflectionSerializer
    private fun Map<*, *>.toJsonObject(): JsonObject = JsonObject(map {
        it.key.toString() to it.value.toJsonElement()
    }.toMap())


    object MapSerializer: KSerializer<Map<String, Any>> {
        override val descriptor: SerialDescriptor
            get() = StringDescriptor.withName("Map")

        override fun deserialize(decoder: Decoder): Map<String, Any> {
            val asJsonObject: JsonObject = decoder.decodeSerializableValue(JsonObjectSerializer)
            return asJsonObject.toPrimitiveMap()
        }

        @ImplicitReflectionSerializer
        override fun serialize(encoder: Encoder, obj: Map<String, Any>) {
            val asJsonObj: JsonObject = obj.toJsonObject()
            encoder.encode(JsonObjectSerializer, asJsonObj)
        }

    }

    object AnySerializer : KSerializer<Any> {
        override val descriptor: SerialDescriptor
            get() = StringDescriptor.withName("Any")


        override fun deserialize(decoder: Decoder): Any {
            val asJsonElement = decoder.decodeSerializableValue(JsonElementSerializer)
            return asJsonElement.toPrimitive()!!

        }

        @ImplicitReflectionSerializer
        override fun serialize(encoder: Encoder, obj: Any) {
            val jsonElement = obj.toJsonElement()
            encoder.encode(JsonElementSerializer, jsonElement)
        }

    }
}