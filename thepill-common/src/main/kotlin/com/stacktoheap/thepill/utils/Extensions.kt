package com.stacktoheap.thepill.utils

import com.stacktoheap.thepill.models.Parameter
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.list
import org.neo4j.graphdb.Node

@UnstableDefault
fun Node.parameters(): List<Parameter> {
    val parametersJsonString = allProperties["parameters"] as String
    return Json.parse(Parameter.serializer().list, parametersJsonString)
}

@UnstableDefault
fun Node.parametersMap(): List<Map<String, Any?>> {
    val parametersJsonString = allProperties["parameters"] as String
    return Json.parse(JsonUtils.MapSerializer.list, parametersJsonString)
}