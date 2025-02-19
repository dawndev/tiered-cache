package com.github.dawndev.tieredcache.redis

import com.github.dawndev.tieredcache.redis.client.RedisTemplate

@FunctionalInterface
interface RedisCallback {
    fun doInRedis(client: RedisTemplate): Any
}