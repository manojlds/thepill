package com.stacktoheap.thepill.models

import com.stacktoheap.thepill.utils.JsonUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.reflect.KClass

@Serializable
enum class ParameterType(val value: String, @Transient val type: KClass<*>) {
    @SerialName("string")
    StringType("string", String::class),
    @SerialName("integer")
    IntType("integer", Long::class),
    @SerialName("long")
    LongType("long", Long::class),
    @SerialName("float")
    FloatType("float", Double::class),
    @SerialName("double")
    DoubleType("double", Double::class),
    @SerialName("boolean")
    BooleanType("boolean", Boolean::class);

    override fun toString(): String{
        return value
    }
}

@Serializable
data class Parameter(val name: String, val type: ParameterType, val possibleValues: Array<@Serializable(with = JsonUtils.AnySerializer::class) Any>? = null) {
    fun values(): List<Any>? {
        return possibleValues?.map { type.type.javaObjectType.cast(it) }
    }

    fun valueFrom(facts: Map<String, Any>): Any {
        return type.type.javaObjectType.cast(facts[name])
    }
}
