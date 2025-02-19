package com.github.dawndev.tieredcache.core

import com.github.dawndev.tieredcache.config.MultiCacheOptions
import com.github.dawndev.tieredcache.redis.client.RedisTemplate

class MultiLevelCacheBuilder private constructor(
) {

    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    class Builder {
        lateinit var client: RedisTemplate
        lateinit var localCache: ICache
        lateinit var remoteCache: ICache
        lateinit var multiCacheSetting: MultiCacheOptions

        fun build(): MultiLevelCache = MultiLevelCache(
            client, localCache, remoteCache, multiCacheSetting
        ).apply {
            //metrics
        }
    }
}
