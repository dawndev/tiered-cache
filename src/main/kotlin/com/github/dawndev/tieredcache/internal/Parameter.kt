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

    fun getRedisLockKey(key: String): String {
        return key + "_sync_lock"
    }

    // 缓存统计和消息推送序列化器
    val GLOBAL_REDIS_SERIALIZER: RedisSerializer = JdkRedisSerializer()

    val REDIS_CHANNEL: String = "tiered-cache-channel"

    val WAIT_TIME = 500 // 刷新缓存等待时间，单位毫秒

    const val REDIS_DEFAULT_PORT = 6379

    const val LOCAL_HOST = "localhost"

    const val REDIS_CONNECT_TIMEOUT = 3600

    const val RELEASE_FAILED = 0L

    const val RELEASE_SUCCESS = 1L

    const val LOCK_SUCCESS = "OK"

    const val UNLOCK_LUA: String = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"
}