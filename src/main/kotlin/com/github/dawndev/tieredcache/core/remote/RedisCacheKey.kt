package com.github.dawndev.tieredcache.core.remote

import com.github.dawndev.tieredcache.redis.serializer.RedisSerializer
import com.github.dawndev.tieredcache.redis.serializer.impl.StringRedisSerializer


class RedisCacheKey(
    val keyElement: Any,
    val serializer: RedisSerializer
) {
    /**
     * 缓存名称
     */
    private var cacheName: String = ""

    /**
     * 是否使用缓存前缀
     */
    private var usePrefix = true

    private val prefixSerializer1: RedisSerializer = StringRedisSerializer()

    /**
     * 获取缓存key
     *
     * @return String
     */
    fun getKey(): String {
        val bytes = getKeyBytes() ?: return ""
        return String(bytes)
    }

    /**
     * 获取key的byte数组
     *
     * @return byte[]
     */
    fun getKeyBytes(): ByteArray? {
        val rawKey = serializeKeyElement() ?: return null
        if (!usePrefix) {
            return rawKey
        }
        val prefix = getPrefix() ?: return null
        val prefixedKey = prefix.copyOf(prefix.size + rawKey.size)
        System.arraycopy(rawKey, 0, prefixedKey, prefix.size, rawKey.size)
        return prefixedKey
    }

    private fun serializeKeyElement(): ByteArray? {
        return if (keyElement is ByteArray) {
            keyElement
        } else serializer.serialize(keyElement)
    }

    /**
     * 获取缓存前缀，默认缓存前缀是":"
     *
     * @return byte[]
     */
    fun getPrefix(): ByteArray? {
        return prefixSerializer1.serialize(if (cacheName.isBlank()) "$cacheName:" else "$cacheName:")
    }


    /**
     * 设置缓存名称
     *
     * @param cacheName cacheName
     * @return RedisCacheKey
     */
    fun cacheName(cacheName: String): RedisCacheKey {
        this.cacheName = cacheName
        return this
    }

    /**
     * 设置是否使用缓存前缀，默认使用
     *
     * @param usePrefix usePrefix
     * @return RedisCacheKey
     */
    fun usePrefix(usePrefix: Boolean): RedisCacheKey {
        this.usePrefix = usePrefix
        return this
    }
}