package com.github.dawndev.tieredcache.metrics

import com.github.dawndev.tieredcache.AbstractCacheManager
import org.slf4j.LoggerFactory

class CacheStatsReporter {

    private val logger = LoggerFactory.getLogger(CacheStatsReporter::class.java)

    fun reportStats() {
        // 获取所有缓存的统计信息
        AbstractCacheManager.cacheManagers.forEach { mgr ->
            mgr.getCacheContainer().forEach { (name, cache) ->
                if (cache is Metrics) {
                    val stats = cache.stats()
                    logger.info(
                        "Cache stats - name: {}, hit rate: {}, avg load time: {}ms",
                        name,
                        String.format("%.2f%%", stats.hitRate * 100),
                        String.format("%.2f", stats.avgLoadPenalty)
                    )
                }
            }

        }
    }
}