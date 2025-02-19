package com.github.dawndev.tieredcache.config


import com.github.dawndev.tieredcache.constg.ExpireModeEnum
import java.util.concurrent.TimeUnit

/**
 * 一级缓存配置项
 *
 * @param initialCapacity 缓存初始Size
 * @param maximumSize     缓存最大Size
 * @param expireTime      缓存有效时间
 * @param timeUnit        缓存时间单位 [TimeUnit]
 * @param expireMode      缓存失效模式[ExpireModeEnum]
 *
 * @author Espresso
 */
data class LocalCacheOptions(
    var initialCapacity: Int = 10,
    var maximumSize: Int = 500,
    var expireTime: Int = 0,
    var timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    var expireMode: ExpireModeEnum = ExpireModeEnum.WRITE
)

/**
 * 二级缓存配置项
 *
 * @param expiration      缓存有效时间
 * @param preloadTime     缓存刷新时间
 * @param timeUnit        时间单位 [TimeUnit]
 * @param forceRefresh    是否强制刷新
 * @param allowNullValue  是否允许存NULL值，模式允许
 * @param magnification   非空值和null值之间的时间倍率
 * @param usePrefix       使用前缀
 *
 * @author Espresso
 */
data class RemoteCacheOptions(
    val expiration: Long = 0L,
    val preloadTime: Long = 0L,
    val timeUnit: TimeUnit = TimeUnit.MICROSECONDS,
    val forceRefresh: Boolean = false,
    val allowNullValue: Boolean = false,
    val magnification: Int = 1,
    val usePrefix: Boolean = true,
)