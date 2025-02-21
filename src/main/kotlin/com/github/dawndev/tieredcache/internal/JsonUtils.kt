package com.github.dawndev.tieredcache.internal

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

 internal object JsonUtils {

    val objectMapper = jacksonObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // 允许删字段
        .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
        .apply {
            //忽略get/set方法，但允许字段属性
            this.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
            this.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            this.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        }

    fun encodeToString(obj: Any?): String {
        // Json.encodeToString(obj)
        return objectMapper.writeValueAsString(obj)
    }

    fun toJSONString(obj: Any?): String {
        return this.encodeToString(obj)
    }

    inline fun <reified T : Any> decodeFromString(json: String): T? {
        return try {
            objectMapper.readValue(json)
            //Json.decodeFromString(serializer<T>(), json)
        } catch (e: Exception) {
            null
        }

    }

    inline fun <reified T : Any> toObj(json: String): T {
        return objectMapper.readValue(json)
    }
}