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
data class ParameterRange(val min: @Serializable(with = JsonUtils.AnySerializer::class)Any, val max: @Serializable(with = JsonUtils.AnySerializer::class)Any, val step: @Serializable(with = JsonUtils.AnySerializer::class) Any)

@Serializable
data class ParameterValues(val displayName: String, val value: @Serializable(with = JsonUtils.AnySerializer::class) Any)

@Serializable
data class ParameterMetadata(val possibleValues: Array<ParameterValues>? = null, val range: ParameterRange? = null) {}

@Serializable
data class Parameter(val name: String, val type: ParameterType, val metadata: ParameterMetadata? = null) {
    fun valueFrom(facts: Map<String, Any>): Any {
        val factValue = facts[name]
        return when  {
            factValue is Long && type == ParameterType.DoubleType  -> factValue.toDouble()
            else ->  type.type.javaObjectType.cast(factValue)
        }
    }
}
