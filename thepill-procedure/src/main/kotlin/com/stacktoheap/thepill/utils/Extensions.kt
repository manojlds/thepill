package com.stacktoheap.thepill.utils

import com.fasterxml.jackson.module.kotlin.readValue
import com.stacktoheap.thepill.models.Parameter
import org.neo4j.graphdb.Node

fun Node.parameters(): List<Parameter> {
    val parametersJsonString = allProperties["parameters"] as String
    return JsonUtils.OBJECT_MAPPER.readValue(parametersJsonString)
}

fun Node.parametersMap(): List<Map<String, Any>> {
    val parametersJsonString = allProperties["parameters"] as String
    return JsonUtils.OBJECT_MAPPER.readValue(parametersJsonString)
}