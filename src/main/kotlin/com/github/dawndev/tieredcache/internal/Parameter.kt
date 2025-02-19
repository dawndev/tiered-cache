package com.github.dawndev.tieredcache.internal

import com.github.dawndev.tieredcache.redis.serializer.RedisSerializer
import com.github.dawndev.tieredcache.redis.serializer.impl.JdkRedisSerializer


const val MESSAGE_KEY: String = "tiered-cache:message-key:%s"

internal object Parameter {

    var NAMESPACE = ""

    fun setNamespace(namespace: String) {
        NAMESPACE = namespace
    }

    fun getMessageRedisKey(): String {
        return String.format(MESSAGE_KEY, NAMESPACE)
    }

    fun getMessageRedisKey(nameSpace: String): String {
        return String.format(MESSAGE_KEY, nameSpace)
    }

    /**
     * 缓存统计和消息推送序列化器
     */
    val GLOBAL_REDIS_SERIALIZER: RedisSerializer = JdkRedisSerializer()
}