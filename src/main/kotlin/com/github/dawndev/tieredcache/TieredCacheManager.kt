//MIT License
//
//Copyright (c) 2025 Espresso
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

package com.github.dawndev.tieredcache

import com.github.dawndev.tieredcache.config.MultiCacheOptions
import com.github.dawndev.tieredcache.core.ICache
import com.github.dawndev.tieredcache.core.MultiLevelCacheBuilder
import com.github.dawndev.tieredcache.core.local.CaffeineLocalCache
import com.github.dawndev.tieredcache.core.remote.RedisRemoteCache
import com.github.dawndev.tieredcache.redis.client.RedisTemplate

/**
 * 多级缓存管理器
 * <p>
 *     这个应该是单例的
 *
 * @property client RedisTemplate
 * @constructor
 */
class TieredCacheManager(
    override var client: RedisTemplate
) : AbstractCacheManager(client) {

    init {
        cacheManagers.add(this)
    }

    override fun getMissingCache(name: String, multiCacheOptions: MultiCacheOptions): ICache? {

        // 创建一级缓存
        val localCache = CaffeineLocalCache(
            name,
            multiCacheOptions.localOptions
        )

        // 创建二级缓存
        val remoteCache = RedisRemoteCache(
            name,
            client,
            multiCacheOptions.remoteOptions
        )

        return MultiLevelCacheBuilder.build {
            this.client = this@TieredCacheManager.client
            this.localCache = localCache
            this.remoteCache = remoteCache
            this.multiCacheSetting = multiCacheOptions
        }
    }
}