package com.github.dawndev.tieredcache.manage

import com.github.dawndev.tieredcache.ICache
import com.github.dawndev.tieredcache.core.MultiLevelCache
import com.github.dawndev.tieredcache.config.MultiCacheOptions
import com.github.dawndev.tieredcache.core.local.CaffeineLocalCache
import com.github.dawndev.tieredcache.core.remote.RedisRemoteCache
import com.github.dawndev.tieredcache.redis.client.RedisTemplate

class MultiLevelCacheManager(
    override var client: RedisTemplate
) : AbstractCacheManager(client){

    init {
        cacheManagers.add(this)
    }

    override fun getMissingCache(name: String, multiCacheOptions: MultiCacheOptions): ICache? {

        // 创建一级缓存
        val caffeineCache = CaffeineLocalCache(
            name, false, multiCacheOptions.l1Options)

        // 创建二级缓存
        val redisCache = RedisRemoteCache(
            name,
            client,
            multiCacheOptions.l2Options
        )

        return MultiLevelCache(
            client,
            caffeineCache,
            redisCache,
            multiCacheOptions
        )
    }
}