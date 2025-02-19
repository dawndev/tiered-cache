package com.github.dawndev.tieredcache.core.remote

import com.github.dawndev.tieredcache.redis.serializer.RedisSerializer
import com.github.dawndev.tieredcache.redis.serializer.impl.StringRedisSerializer

class RedisCacheKey private constructor(
    val keyElement: Any,
    private val serializer: RedisSerializer,
    private val cacheName: String,
    private val usePrefix: Boolean,
    private val prefixSerializer: RedisSerializer
): RemoteKey {

    /**
     * 获取缓存key
     *
     * @return String
     */
    override fun getKey(): String {
        val bytes = this.getKeyBytes() ?: return ""
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
        val prefixedKey = ByteArray(prefix.size + rawKey.size)
        System.arraycopy(prefix, 0, prefixedKey, 0, prefix.size)
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
    private fun getPrefix(): ByteArray? {
        val prefix = if (cacheName.isBlank()) ":"
        else "$cacheName:"
        return prefixSerializer.serialize(prefix)
    }

    companion object {
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    class Builder {
        lateinit var keyElement: Any
        var serializer: RedisSerializer = StringRedisSerializer()
        var cacheName: String = ""
        var enablePrefix: Boolean = true
        var prefixSerializer: RedisSerializer = StringRedisSerializer()

        fun build(): RedisCacheKey = RedisCacheKey(
            keyElement, serializer, cacheName, enablePrefix, prefixSerializer
        ).apply {
            // pass
        }
    }

}