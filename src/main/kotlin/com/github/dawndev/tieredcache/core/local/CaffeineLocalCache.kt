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
import com.github.dawndev.tieredcache.internal.*
import com.github.dawndev.tieredcache.internal.JsonUtils
import com.github.dawndev.tieredcache.internal.fromStoredValue
import com.github.dawndev.tieredcache.internal.taskIfDebug
import com.github.dawndev.tieredcache.metrics.CacheMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
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

    // 缓存指标
    val metrics: CacheMetrics by lazy {
        CacheMetrics(SimpleMeterRegistry(), name)
    }

    private val caffeine: Cache<Any, Any> by lazy {
        val builder = Caffeine.newBuilder()
        builder.initialCapacity(initialCapacity)
        builder.maximumSize(maximumSize)
        builder.softValues()
        if (enableStats) {
            // 开启统计, 对性能有一定影响, 生产建议关闭
            builder.recordStats()
        }
        when (expireMode) {
            ExpireModeEnum.WRITE -> builder.expireAfterWrite(Duration.ofMillis(expireTime))
            ExpireModeEnum.ACCESS -> builder.expireAfterAccess(Duration.ofMillis(expireTime))
        }
        // 根据Caffeine builder创建 Cache 对象
        logger.debug("caffeine init, enableStats:$enableStats")
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
        get() = this.caffeine


    override fun <T> get(key: String, resultType: Class<T>): T? {
        logger.taskIfDebug("caffeine缓存 key={} 获取缓存", key)
        return if (caffeine is LoadingCache<*, *>) {
            (caffeine as LoadingCache<Any?, Any?>)[key] as T?
        } else caffeine.getIfPresent(key) as T?
    }

    @SuppressWarnings("unchecked")
    override fun <T> get(key: String, resultType: Class<T>, valueLoader: Callable<T>): T? {
        logger.taskIfDebug("caffeine缓存 key={} 获取缓存， 如果没有命中就走库加载缓存", key)
        val result = caffeine[key, { _ -> loaderValue(key, valueLoader) }]

        // 如果不允许存NULL值 直接删除NULL值缓存
        val isEvict = !enableNull && (result == null || result is NullValue)
        if (isEvict) {
            evict(key)
        }
        return result.fromStoredValue(enableNull) as T?
    }

    override fun put(key: String, value: Any?) {
        // 允许存NULL值
        if (enableNull) {
            logger.taskIfDebug("caffeine缓存 key={} put缓存，缓存值：{}", key, JsonUtils.toJSONString(value))
            this.storeValue(key, value)
            return
        }

        // 不允许存NULL值
        if (value != null && value !is NullValue) {
            logger.taskIfDebug("caffeine缓存 key={} put缓存，缓存值：{}", key, JsonUtils.toJSONString(value))

            this.storeValue(key, value)
            return
        }
        logger.debug("缓存值为NULL并且不允许存NULL值，不缓存数据")
    }

    override fun <T> putIfAbsent(key: String, value: Any?, resultType: Class<T>): T? {
        logger.taskIfDebug("caffeine缓存 key={} putIfAbsent 缓存，缓存值：{}", key, JsonUtils.toJSONString(value))

        val flag = !enableNull && (value == null || value is NullValue)
        if (flag) {
            return null
        }
        val result = caffeine[key, { _ -> value.toStoredValue(enableNull) }]
        return result.fromStoredValue(enableNull) as T?
    }

    override fun evict(key: String) {
        logger.taskIfDebug("caffeine缓存 key={} 清除缓存", key)
        caffeine.invalidate(key)
        metrics.updateCacheSize(caffeine.estimatedSize())
    }

    override fun clear() {
        logger.debug("caffeine缓存 name={} 清空缓存", name)
        caffeine.invalidateAll()
        metrics.updateCacheSize(caffeine.estimatedSize())
    }


    /**
     * 加载数据
     */
    private fun <T> loaderValue(key: Any, valueLoader: Callable<T>): Any? {

        val sample = this.metrics.startTimer()
        return try {
            val t = valueLoader.call()
            logger.taskIfDebug("caffeine缓存 key={} 从库加载缓存{}", key, JsonUtils.toJSONString(t))
            t.toStoredValue(enableNull)
        } catch (e: Exception) {
            this.metrics.recordLoadError(name)
            throw CacheLoadException(key, e)
        } finally {
            this.metrics.stopTimer(sample)
        }
    }

    private fun storeValue(key: String, userValue: Any?) {
        val v = userValue.toStoredValue(enableNull)
        if (v == null) {
            return
        }
        caffeine.put(key, v)
    }

    override fun estimatedSize(): Long {
        return caffeine.estimatedSize()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CaffeineLocalCache::class.java)
    }
}