package com.github.dawndev.tieredcache.redis.serializer.impl

import com.github.dawndev.tieredcache.exception.SerializationException
import com.github.dawndev.tieredcache.internal.JsonUtils
import com.github.dawndev.tieredcache.redis.serializer.AbstractRedisSerializer
import java.io.*
import java.util.*


/**
 * JDK 序列化方式
 *
 * @author jdg
 */
class JdkRedisSerializer : AbstractRedisSerializer() {

    @Throws(SerializationException::class)
    override fun <T> serialize(value: T?): ByteArray? {
        if (value == null) {
            return EMPTY_ARRAY
        }
        if (value !is Serializable) {
            throw IllegalArgumentException(this.javaClass.simpleName.toString() + " requires a Serializable payload but received an object of type [" + value.javaClass.name + "]")
        }
        try {
            ByteArrayOutputStream(1024).use { outputStream ->
                ObjectOutputStream(outputStream).use { objectOutputStream ->
                    objectOutputStream.writeObject(value)
                    objectOutputStream.flush()
                    return outputStream.toByteArray()
                }
            }
        } catch (e: Exception) {
            throw SerializationException(String.format("JdkRedisSerializer 序列化异常: %s, 【%s】", e.message, JsonUtils.toJSONString(value)), e)
        }
    }

    @Throws(SerializationException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(bytes: ByteArray?, resultType: Class<T>): T? {
        if (super.isEmpty(bytes)) {
            return null
        }
        if (super.getNullValueBytes().contentEquals(bytes)) {
            return null
        }
        try {
            ByteArrayInputStream(bytes).use { byteStream -> ObjectInputStream(byteStream).use { stream -> return stream.readObject() as T } }
        } catch (e: Exception) {
            throw SerializationException(String.format("JdkRedisSerializer 反序列化异常: %s, 【%s】", e.message, bytes), e)
        }
    }
}