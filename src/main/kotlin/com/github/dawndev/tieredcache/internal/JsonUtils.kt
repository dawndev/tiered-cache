package com.github.dawndev.tieredcache.internal

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

internal object JsonUtils {

    fun encodeToString(obj: Any?): String {
        return Json.encodeToString(obj)
    }

    fun toJSONString(obj: Any?): String {
        return this.encodeToString(obj)
    }

    inline fun <reified T : Any> decodeFromString(json: String): T? {
        return try {
            Json.decodeFromString(serializer<T>(), json)
        } catch (e: Exception) {
            null
        }

    }
}

//@Serializable
//data class Test(
//    var code: Int
//)