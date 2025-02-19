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
import com.github.dawndev.tieredcache.listener.RedisMessageListener
import com.github.dawndev.tieredcache.listener.RedisMessagePullTask
import com.github.dawndev.tieredcache.redis.client.RedisTemplate
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * 公共的抽象 [CacheManager] 的实现.
 *
 * @author Espresso
 */
abstract class AbstractCacheManager(
    open var client: RedisTemplate
) : CacheManager {

    /**
     * 缓存容器
     * <p>
     *     外层key是cache_name
     *     里层key是[一级缓存有效时间-二级缓存有效时间]
     */
    private val cacheContainer: ConcurrentMap<String, ConcurrentMap<String, ICache>> = ConcurrentHashMap(16)

    /**
     * 缓存名称容器
     */
    @Volatile
    private var cacheNames: MutableSet<String> = LinkedHashSet()

    override fun getCache(name: String): Collection<ICache> {
        val cacheMap = cacheContainer[name]
        return if (cacheMap.isNullOrEmpty()) {
            emptyList()
        } else cacheMap.values
    }

    /**
     * Lazy cache initialization on access
     * @param name String
     * @param multiCacheOptions MultiCacheOptions
     * @return Cache?
     */
    override fun getCache(name: String, multiCacheOptions: MultiCacheOptions): ICache? {

        // 第一次获取缓存Cache，如果有直接返回,如果没有加锁往容器里里面放Cache
        var cacheMap = cacheContainer[name]
        if (!cacheMap.isNullOrEmpty()) {
            val cache = cacheMap[multiCacheOptions.internalKey]
            if (cache != null) {
                return cache
            }
        }

        // 第二次获取缓存Cache，加锁往容器里里面放Cache
        synchronized(cacheContainer) {
            cacheMap = cacheContainer[name]
            if (!cacheMap.isNullOrEmpty()) {
                // 从容器中获取缓存
                val cache = cacheMap!![multiCacheOptions.internalKey]
                if (cache != null) {
                    return cache
                }
            } else {
                cacheMap = ConcurrentHashMap(16)
                cacheContainer[name] = cacheMap
                // 更新缓存名称
                updateCacheNames(name)
            }

            // 新建一个Cache对象
            var cache = this.getMissingCache(name, multiCacheOptions)
            if (cache != null) {
                // 装饰Cache对象
                cache = this.decorateCache(cache)
                // 将新的Cache对象放到容器
                cacheMap!![multiCacheOptions.internalKey] = cache
                if (cacheMap!!.size > 1) {

                    logger.warn("缓存名称为 {} 的缓存,存在两个不同的过期时间配置，请一定注意保证缓存的key唯一性，否则会出现缓存过期时间错乱的情况", name)
                }
            }
            return cache
        }
    }

    override fun getCacheNames(): Collection<String> {
        return cacheNames
    }

    /**
     * 更新缓存名称容器
     *
     * @param name 需要添加的缓存名称
     */
    private fun updateCacheNames(name: String) {
        cacheNames.add(name)
    }

    /**
     * 获取Cache对象的装饰示例
     *
     * @param cache 需要添加到CacheManager的Cache实例
     * @return 装饰过后的Cache实例
     */
    protected open fun decorateCache(cache: ICache): ICache {
        return cache
    }

    /**
     * 根据缓存名称在CacheManager中没有找到对应Cache时，通过该方法新建一个对应的Cache实例
     *
     * @param name                  缓存名称
     * @param multiCacheOptions     缓存配置
     * @return [ICache]
     */
    protected abstract fun getMissingCache(name: String, multiCacheOptions: MultiCacheOptions): ICache?

    /**
     * 获取缓存容器
     *
     * @return 返回缓存容器
     */
    fun getCacheContainer(): ConcurrentMap<String, ConcurrentMap<String, ICache>> {
        return cacheContainer
    }

    @Throws(Exception::class)
    fun afterPropertiesSet() {

        //  redis pull 消息任务
        RedisMessagePullTask(this)

        //  redis pub/sub 监听器
        RedisMessageListener(this)
    }

    @Throws(Exception::class)
    fun destroy() {
        //RedisPubSubThreadTaskUtils.close()
        //BeanFactory.getBean(StatsService::class.java).shutdownExecutor()
    }

    companion object {
        val cacheManagers: HashSet<AbstractCacheManager> = LinkedHashSet()
        private val logger = LoggerFactory.getLogger(AbstractCacheManager::class.java)
    }
}
