package com.github.dawndev.tieredcache.config

import com.github.dawndev.tieredcache.constg.RedisMessageEnum
import com.github.dawndev.tieredcache.internal.Parameter

/**
 * Redis data source
 *
 * @param database
 * @param cluster           不为空表示集群版
 * @param host
 * @param port
 * @param password
 * @param timeout
 */
data class RedisConfigure(
    val database: Int,
    val cluster: String? = "",
    val host: String = Parameter.LOCAL_HOST,
    val port: Int = Parameter.REDIS_DEFAULT_PORT,
    val password: String = "",
    val timeout: Int = Parameter.REDIS_CONNECT_TIMEOUT,
)

/**
 * Redis pub sub message
 *
 * @param cacheName         缓存名称
 * @param key               缓存名称
 * @param messageType       消息类型
 * @param source            消息来源
 */
data class RedisPubSubMessage(
    val cacheName: String,
    val key: String,
    val messageType: RedisMessageEnum,
    val source: String = ""
) {

    companion object {
        val SOURCE: String = "web-manage"
    }
}