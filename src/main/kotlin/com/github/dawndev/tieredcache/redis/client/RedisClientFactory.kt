package com.github.dawndev.tieredcache.redis.client

import com.github.dawndev.tieredcache.config.RedisConfigure

/**
 * Redis factory
 *
 * @constructor Create empty Redis factory
 */
object RedisClientFactory {

    fun createRedis(properties: RedisConfigure): RedisTemplate {
        return if (properties.cluster.isNotBlank()) {
            ShardedRedisTemplate(properties)
        } else {
            SingleRedisTemplate(properties)
        }
    }
}