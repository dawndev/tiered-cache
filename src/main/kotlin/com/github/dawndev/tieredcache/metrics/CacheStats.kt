package com.github.dawndev.tieredcache.metrics

data class CacheStats(
    var cacheName: String?, // 缓存名称
    var hitCount: Long = 0, // 命中次数
    var missCount: Long = 0, // 未命中次数
    var loadSuccessCount: Long = 0, // 加载成功次数
    var loadFailureCount: Long = 0, // 加载失败次数
    var hitRate: Double = 0.0, // 命中率
    var avgLoadPenalty: Double = 0.0 // 平均加载耗时
) {

    companion object {

        fun from(cacheName: String, caffeineStats: com.github.benmanes.caffeine.cache.stats.CacheStats): CacheStats {
            val stats = CacheStats(
                cacheName,
                caffeineStats.hitCount(),
                caffeineStats.missCount(),
                caffeineStats.loadSuccessCount(),
                caffeineStats.loadFailureCount(),
                caffeineStats.hitRate(),
                caffeineStats.averageLoadPenalty()
            )
            return stats
        }
    }
}