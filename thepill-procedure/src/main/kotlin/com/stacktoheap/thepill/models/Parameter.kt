package com.stacktoheap.thepill.models

import kotlin.reflect.KClass

enum class ParameterType(val value: String, val type: KClass<*>) {
    StringType("string", String::class),
    IntType("integer", Integer::class),
    DoubleType("double", Double::class),
    BooleanType("boolean", Boolean::class);

    override fun toString(): String{
        return value
    }
}

class Parameter(val name: String, val type: ParameterType, val possibleValues: Array<Any>? = null) {
    fun values(): List<Any>? {
        return possibleValues?.map { type.type.javaObjectType.cast(it) }
    }

    fun valueFrom(facts: Map<String, Any>): Any {
        return type.type.javaObjectType.cast(facts[name])
    }
}