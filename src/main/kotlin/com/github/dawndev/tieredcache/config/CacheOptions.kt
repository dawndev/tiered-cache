package com.github.dawndev.tieredcache.config


import com.github.dawndev.tieredcache.constg.CacheOption
import com.github.dawndev.tieredcache.constg.ExpireModeEnum

/**
 * 一级缓存配置项
 *
 * @param initialCapacity 缓存初始Size
 * @param maximumSize     缓存最大Size
 * @param expiration      缓存有效时间(ms)
 * @param expireMode      缓存失效模式[ExpireModeEnum]
 *
 * @author Espresso
 */
data class LocalCacheOptions(
    var initialCapacity: Int = 10,
    var maximumSize: Long = 500,
    var expiration: Long = 0,
    var expireMode: ExpireModeEnum = ExpireModeEnum.WRITE,
    var enableNull: Boolean = false,
    var enableStats: Boolean = false,
) {
    fun <T> option(option: CacheOption, value: T) {
        when (option) {
            CacheOption.INIT_CAPACITY -> initialCapacity = value as Int
            CacheOption.MAXIMUM_SIZE -> maximumSize = value as Long
            CacheOption.MILLIS_EXPIRE -> expiration = value as Long
            CacheOption.MODE_EXPIRE -> expireMode = value as ExpireModeEnum
            CacheOption.ENABLE_NULL -> enableNull = value as Boolean
            CacheOption.ENABLE_STATS -> enableStats = value as Boolean
            else -> throw IllegalArgumentException("LocalCacheOptions not support option $option")
        }
    }
}

/**
 * 二级缓存配置项
 *
 * @param expiration      缓存有效时间(ms)
 * @param preloadTime     缓存刷新时间(ms)
 * @param enableForceRefresh    是否强制刷新
 * @param enableNull      是否允许存NULL值，模式允许
 * @param magnification   非空值和null值之间的时间倍率
 * @param enablePrefix       使用前缀
 *
 * @author Espresso
 */
data class RemoteCacheOptions(
    var expiration: Long = 0L,
    var preloadTime: Long = 0L,
    var magnification: Int = 1,
    var enableForceRefresh: Boolean = false,
    var enableNull: Boolean = false,
    var enablePrefix: Boolean = true,
) {
    fun <T> option(option: CacheOption, value: T) {
        when (option) {
            CacheOption.MILLIS_PRELOAD -> preloadTime = value as Long
            CacheOption.ENABLE_FORCE_REFRESH -> enableForceRefresh = value as Boolean
            CacheOption.MILLIS_EXPIRE -> expiration = value as Long
            CacheOption.ENABLE_NULL -> enableNull = value as Boolean
            CacheOption.MAGNIFICATION -> magnification = value as Int
            CacheOption.ENABLE_PREFIX -> enablePrefix = value as Boolean
            else -> throw IllegalArgumentException("RemoteCacheOptions not support option $option")
        }
    }
}