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

    private val logger = LoggerFactory.getLogger(AbstractCacheManager::class.java)

    // 缓存容器
    private val cacheContainers: ConcurrentMap<String, ICache> = ConcurrentHashMap(16)

    /**
     * 缓存名称容器
     */
    @Volatile
    private var cacheNames: MutableSet<String> = LinkedHashSet()

    override fun getCache(name: String): ICache? {
        val cache = this.cacheContainers[name]
        return cache
    }

    /**
     * Lazy cache initialization on access
     * @param name String
     * @param multiCacheOptions MultiCacheOptions
     * @return Cache?
     */
    override fun registerCache(name: String, multiCacheOptions: MultiCacheOptions): ICache? {
        val cache = this.getCache(name)
        if (null != cache) {
            logger.warn("name[{}] 已经注册过了~", name)
            return cache
        }

        // 第一次获取缓存Cache，如果有直接返回,如果没有加锁往容器里里面放Cache
        synchronized(cacheContainers) {
            // 新建一个Cache对象
            var instance = this.getMissingCache(name, multiCacheOptions)
            if (instance != null) {
                // 装饰Cache对象
                instance = this.decorateCache(instance)
                cacheContainers[name] = instance
                this.updateCacheNames(name)
            }
            logger.debug("成功新建cache， {}", name)
            return instance
        }

    }

    override fun getCacheNames(): Collection<String> {
        return cacheNames
    }

    override fun unregisterCache(name: String) {
        cacheContainers.remove(name)
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
    fun getCacheContainer(): ConcurrentMap<String, ICache> {
        return cacheContainers
    }

    @Throws(Exception::class)
    fun afterPropertiesSet() {

        //  redis pull 消息任务
        RedisMessagePullTask(this)

        //  redis pub/sub 监听器
        RedisMessageListener(this)
    }

    @Throws(Exception::class)
    override fun destroy() {
        //RedisPubSubThreadTaskUtils.close()
        //BeanFactory.getBean(StatsService::class.java).shutdownExecutor()
    }

    companion object {
        val cacheManagers: HashSet<AbstractCacheManager> = LinkedHashSet()
    }
}
