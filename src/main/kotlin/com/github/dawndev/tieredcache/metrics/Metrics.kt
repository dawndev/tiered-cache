package com.github.dawndev.tieredcache.metrics

interface Metrics {
    fun stats(): CacheStats
}