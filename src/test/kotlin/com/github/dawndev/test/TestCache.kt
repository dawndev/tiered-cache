package com.github.dawndev.test

import com.github.dawndev.tieredcache.TieredCacheManager
import com.github.dawndev.tieredcache.config.*
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

        val multiCacheOptions = MultiCacheOptions.Builder()
            .localOptions(localCacheOption)
            .remoteOptions(remoteCacheOption)
            .enableLocal(true)
            .build()
//        val cacheName = "cache:name"

//        val cacheManager = TieredCacheManager(redisClient)
//        cacheManager.getCache(cacheName, multiCacheOptions)?.let {
//            it.put("test", 1)
//            it.get("test", Int::class.java)
//        }
    }
}