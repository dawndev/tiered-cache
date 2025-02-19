package com.github.dawndev.tieredcache.manage

import com.github.dawndev.tieredcache.ICache
import com.github.dawndev.tieredcache.config.MultiCacheOptions

/**
 * 缓存管理器
 * 允许通过缓存名称来获的对应的 [ICache].
 *
 * @author jdg
 */
interface CacheManager {

    /**
     * 根据缓存名称返回对应的[Collection].
     *
     * @param name 缓存的名称 (不能为 `null`)
     * @return 返回对应名称的Cache, 如果没找到返回 `null`
     */
    fun getCache(name: String): Collection<ICache>

    /**
     * 根据缓存名称返回对应的[ICache]，如果没有找到就新建一个并放到容器
     *
     * @param name                  缓存名称
     * @param multiCacheOptions     多级缓存配置
     * @return [ICache]
     */
    fun getCache(name: String, multiCacheOptions: MultiCacheOptions): ICache?

    /**
     * 获取所有缓存名称的集合
     *
     * @return 所有缓存名称的集合
     */
    fun getCacheNames(): Collection<String>
}