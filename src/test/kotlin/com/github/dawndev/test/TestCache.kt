package com.github.dawndev.test

import com.github.dawndev.tieredcache.TieredCacheManager
import com.github.dawndev.tieredcache.config.LocalCacheOptions
import com.github.dawndev.tieredcache.config.MultiCacheOptions
import com.github.dawndev.tieredcache.config.RedisConfigure
import com.github.dawndev.tieredcache.config.RemoteCacheOptions
import com.github.dawndev.tieredcache.constg.CacheOption
import com.github.dawndev.tieredcache.redis.client.RedisClientFactory
import org.junit.jupiter.api.Test


class TestCache {

    @Test
    fun test() {
        val localCacheOption = LocalCacheOptions()
        with(localCacheOption) {
            option(CacheOption.INIT_CAPACITY, 5)
            option(CacheOption.MAXIMUM_SIZE, 10L)
        }
        val remoteCacheOption = RemoteCacheOptions()
        with(remoteCacheOption) {
            option(CacheOption.MILLIS_EXPIRE, 5000L)
        }
        val multiCacheOptions: MultiCacheOptions = MultiCacheOptions(
            localCacheOption,
            remoteCacheOption,
            true
        )
        val cacheName = "cache:name"
//        val redisClient = RedisClientFactory.createRedis(
//            RedisConfigure(
//                0,
//                "",
//                "192.168.70.150",
//                32379,
//            )
//        )
//        val cacheManager = TieredCacheManager(redisClient)
    }
}