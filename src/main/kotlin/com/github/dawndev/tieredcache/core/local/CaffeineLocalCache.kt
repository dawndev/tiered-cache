package com.github.dawndev.tieredcache.core.local

import com.github.dawndev.tieredcache.core.AbstractCache
import com.github.dawndev.tieredcache.config.LocalCacheOptions
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.github.dawndev.tieredcache.constg.ExpireModeEnum
import com.github.dawndev.tieredcache.core.LocalCache
import com.github.dawndev.tieredcache.exception.CacheLoadException
import com.github.dawndev.tieredcache.internal.JsonUtils
import com.github.dawndev.tieredcache.internal.NullValue
import java.time.Duration

/**
 * 使用name和[LocalCacheOptions]创建一个 [CaffeineLocalCache] 实例
 *
 * @param name              缓存名称
 * @param options         一级缓存配置 [LocalCacheOptions]
 *
 */
@Suppress("UNCHECKED_CAST")
class CaffeineLocalCache(
    override val name: String,
    override val enableNull: Boolean,
    private val initialCapacity: Int,
    private val maximumSize: Long,
    private val expireMode: ExpireModeEnum,
    private val expireTime: Long,
    private val enableStats: Boolean
) : AbstractCache(name, enableNull), LocalCache {

    private val cache: Cache<Any, Any> by lazy {
        val builder = Caffeine.newBuilder()
        builder.initialCapacity(initialCapacity)
        builder.maximumSize(maximumSize)
        builder.softValues()
        when (expireMode) {
            ExpireModeEnum.WRITE -> builder.expireAfterWrite(Duration.ofMillis(expireTime))
            ExpireModeEnum.ACCESS -> builder.expireAfterAccess(Duration.ofMillis(expireTime))
        }
        // 根据Caffeine builder创建 Cache 对象
        logger.debug("caffeine init~")
        builder.build()
    }

    constructor(
        name: String,
        options: LocalCacheOptions
    ): this(
        name,
        options.enableNull,
        options.initialCapacity,
        options.maximumSize,
        options.expireMode,
        options.expiration,
        options.enableStats
    )


    override val nativeRef: Any
        get() = this.cache


    override fun <T> get(key: String, resultType: Class<T>): T? {
        if (logger.isDebugEnabled) {
            logger.debug("caffeine缓存 key={} 获取缓存", key)
        }

        return if (cache is LoadingCache<*, *>) {
            (cache as LoadingCache<Any?, Any?>)[key] as T?
        } else cache.getIfPresent(key) as T?
    }

    @SuppressWarnings("unchecked")
    override fun <T> get(key: String, resultType: Class<T>, valueLoader: Callable<T>): T? {
        if (logger.isDebugEnabled) {
            logger.debug("caffeine缓存 key={} 获取缓存， 如果没有命中就走库加载缓存", key)
        }

        val result = cache[key, { _ -> loaderValue(key, valueLoader) }]

        // 如果不允许存NULL值 直接删除NULL值缓存
        val isEvict = !enableNull && (result == null || result is NullValue)
        if (isEvict) {
            evict(key)
        }
        return fromStoreValue(result) as T?
    }

    override fun put(key: String, value: Any?) {
        // 允许存NULL值
        if (enableNull) {
            if (logger.isDebugEnabled) {
                logger.debug("caffeine缓存 key={} put缓存，缓存值：{}", key, JsonUtils.toJSONString(value))
            }
            this.storeValue(key, value)
            return
        }

        // 不允许存NULL值
        if (value != null && value !is NullValue) {
            if (logger.isDebugEnabled) {
                logger.debug("caffeine缓存 key={} put缓存，缓存值：{}", key, JsonUtils.toJSONString(value))
            }
            this.storeValue(key, value)
            return
        }
        logger.debug("缓存值为NULL并且不允许存NULL值，不缓存数据")
    }

    override fun <T> putIfAbsent(key: String, value: Any?, resultType: Class<T>): T? {
        if (logger.isDebugEnabled) {
            logger.debug("caffeine缓存 key={} putIfAbsent 缓存，缓存值：{}", key, JsonUtils.toJSONString(value))
        }
        val flag = !enableNull && (value == null || value is NullValue)
        if (flag) {
            return null
        }
        val result = cache[key, { _ -> toStoreValue(value) }]
        return fromStoreValue(result) as T?
    }

    override fun evict(key: String) {
        if (logger.isDebugEnabled) {
            logger.debug("caffeine缓存 key={} 清除缓存", key)
        }
        cache.invalidate(key)
    }

    override fun clear() {
        logger.debug("caffeine缓存 name={} 清空缓存", name)
        cache.invalidateAll()
    }


    /**
     * 加载数据
     */
    private fun <T> loaderValue(key: Any, valueLoader: Callable<T>): Any? {

        return try {
            val t = valueLoader.call()
            if (logger.isDebugEnabled) {
                logger.debug("caffeine缓存 key={} 从库加载缓存{}", key, JsonUtils.toJSONString(t))
            }

            toStoreValue(t)
        } catch (e: Exception) {
            throw CacheLoadException(key, e)
        }
    }

    private fun storeValue(key: String, userValue: Any?) {
        val v = super.toStoreValue(userValue)
        if (v == null) {
            return
        }
        cache.put(key, v)
    }

    override fun estimatedSize(): Long {
        return cache.estimatedSize()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CaffeineLocalCache::class.java)
    }
}