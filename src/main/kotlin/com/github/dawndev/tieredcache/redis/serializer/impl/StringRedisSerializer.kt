package com.github.dawndev.tieredcache.redis.serializer.impl

import com.github.dawndev.tieredcache.exception.SerializationException
import com.github.dawndev.tieredcache.redis.serializer.RedisSerializer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets


/**
 * 必须重写序列化器，否则@Cacheable注解的key会报类型转换错误
 *
 * @author yuhao.wang
 */
class StringRedisSerializer(
    val charset: Charset = StandardCharsets.UTF_8
) : RedisSerializer {

    @Throws(SerializationException::class)
    override fun <T> serialize(value: T?): ByteArray? {
        if (value == null) {
            return null
        }
        if (value is String) {
            return (value as String).toByteArray(charset)
        }
        throw UnsupportedOperationException("String序列化方式不支持其他数据类型的序列化")
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(SerializationException::class)
    override fun <T> deserialize(bytes: ByteArray?, resultType: Class<T>): T? {
        return deserialize(bytes) as T
    }

    @Throws(SerializationException::class)
    fun deserialize(bytes: ByteArray?): String? {
        return bytes?.let { String(it, charset) }
    }
}
