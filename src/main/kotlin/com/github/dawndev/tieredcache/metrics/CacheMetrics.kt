package com.github.dawndev.tieredcache.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.util.concurrent.atomic.AtomicLong


/**
 * 缓存指标
 * @property registry 指标注册器
 * @property cacheName 缓存名称
 */
class CacheMetrics(
    val registry: MeterRegistry,
    val cacheName: String
) {
    // 缓存加载异常计数器
    private val cacheLoadExceptionCounter: Counter by lazy {
        registry.counter(
            CACHE_LOAD_ERRORS, CACHE_NAME_TAG,
            cacheName
        )
    }

    // 缓存加载耗时计时器
    private val cacheLoadTimer: Timer by lazy {
        val t = registry.timer(CACHE_LOAD_TIME, CACHE_NAME_TAG, cacheName)
        Gauge
            .builder<CacheMetrics>(
                CACHE_SIZE, this
            ) { obj: CacheMetrics -> obj.getCurrentSize().toDouble().toDouble() }
            .tag(CACHE_NAME_TAG, cacheName!!)
            .register(registry)
        Gauge
            .builder<CacheMetrics>(
                CACHE_HIT_COUNT, this
            ) { obj: CacheMetrics -> obj.getHitCount().toDouble().toDouble() }
            .tag(CACHE_NAME_TAG, cacheName)
            .register(registry)
        Gauge
            .builder<CacheMetrics>(
                CACHE_HIT_RATE, this
            ) { obj: CacheMetrics -> obj.hitRate }
            .tag(CACHE_NAME_TAG, cacheName)
            .register(registry)
        t
    }

    // 缓存大小指标
    private val currentSize: AtomicLong = AtomicLong(0)

    // 缓存命中率
    private val hitCount: AtomicLong = AtomicLong(0)


    /**
     * 记录缓存加载异常
     */
    fun recordLoadError(cacheName: String?) {
        cacheLoadExceptionCounter.increment()
    }

    /**
     * 开始缓存加载计时
     *
     * @return 计时器样本
     */
    fun startTimer(): Timer.Sample {
        return Timer.start()
    }

    /**
     * 停止缓存加载计时
     *
     * @param sample 计时器样本
     */
    fun stopTimer(sample: Timer.Sample) {
        sample.stop(cacheLoadTimer)
    }

    /**
     * 更新缓存大小
     *
     * @param size 当前缓存大小
     */
    fun updateCacheSize(size: Long) {
        currentSize.set(size)
    }

    /**
     * 获取当前缓存大小
     */
    fun getCurrentSize(): Long {
        return currentSize.get()
    }

    val cacheSize: Long
        /**
         * 获取当前缓存大小
         */
        get() = currentSize.get()

    /**
     * 增加缓存大小
     *
     * @param size 增加的大小
     */
    fun addCacheSize(size: Long) {
        currentSize.addAndGet(size)
    }

    /**
     * 减少缓存大小
     *
     * @param size 减少的大小
     */
    fun subCacheSize(size: Long) {
        currentSize.addAndGet(-size)
    }

    /**
     * 增加缓存命中次数
     *
     * @param hit 命中次数
     */
    fun addHitCount(hit: Long) {
        hitCount.addAndGet(hit)
    }

    /**
     * 获取缓存命中次数
     */
    fun getHitCount(): Long {
        return hitCount.get()
    }

    val hitRate: Double
        /**
         * 获取缓存命中率
         */
        get() = hitCount.get().toDouble() / currentSize.get()

    companion object {
        /**
         * 缓存名称标签
         */
        private const val CACHE_NAME_TAG = "cacheName"
    }
}

// 缓存加载错误
private const val CACHE_LOAD_ERRORS = "tiered.cache.load.errors"

// 缓存加载时间
private const val CACHE_LOAD_TIME = "tiered.cache.load.time"

// 缓存大小
private const val CACHE_SIZE = "tiered.cache.size"

// 缓存大小
private const val CACHE_HIT_RATE = "tiered.cache.hit.rate"

// 缓存命中次数
private const val CACHE_HIT_COUNT = "tiered.cache.hit.count"
