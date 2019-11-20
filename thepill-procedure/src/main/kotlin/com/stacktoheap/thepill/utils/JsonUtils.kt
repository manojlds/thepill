package com.stacktoheap.thepill.utils

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

object JsonUtils {
    val OBJECT_MAPPER: ObjectMapper by lazy  {
        val mapper = ObjectMapper().registerModule(KotlinModule())
        mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        mapper
    }
}