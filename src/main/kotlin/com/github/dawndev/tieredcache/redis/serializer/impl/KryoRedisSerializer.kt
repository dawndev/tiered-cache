package com.github.dawndev.tieredcache.redis.serializer.impl

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.github.dawndev.tieredcache.exception.SerializationException
import com.github.dawndev.tieredcache.internal.JsonUtils
import com.github.dawndev.tieredcache.redis.serializer.AbstractRedisSerializer
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * kryo 序列化方式
 *
 * @author jdg
 */
class KryoRedisSerializer : AbstractRedisSerializer() {

    @Throws(SerializationException::class)
    override fun <T> serialize(value: T?): ByteArray? {
        if (value == null) {
            return EMPTY_ARRAY
        }
        val kryo = KRYO.get()

        // 设置成false 序列化速度更快，但是遇到循环应用序列化器会报栈内存溢出
        kryo.references = false
        kryo.register(value.javaClass)
        try {
            ByteArrayOutputStream().use { baos ->
                Output(baos).use { output ->
                    kryo.writeClassAndObject(output, value)
                    output.flush()
                    return baos.toByteArray()
                }
            }
        } catch (e: Exception) {
            throw SerializationException(String.format("KryoRedisSerializer 序列化异常: %s, 【%s】", e.message, JsonUtils.toJSONString(value)), e)
        } finally {
            KRYO.remove()
        }
    }

    @Throws(SerializationException::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T> deserialize(bytes: ByteArray?, resultType: Class<T>): T? {
        if (super.isEmpty(bytes)) {
            return null
        }
        if (Arrays.equals(getNullValueBytes(), bytes)) {
            return null
        }
        val kryo = KRYO.get()
        // 设置成false 序列化速度更快，但是遇到循环应用序列化器会报栈内存溢出
        kryo.references = false
        kryo.register(resultType)
        try {
            Input(bytes).use { input ->
                val result = kryo.readClassAndObject(input)
                return result as T
            }
        } catch (e: Exception) {
            throw SerializationException(String.format("KryoRedisSerializer 反序列化异常: %s, 【%s】", e.message, bytes), e)
        } finally {
            KRYO.remove()
        }
    }

    companion object {
        val KRYO = ThreadLocal.withInitial { Kryo() }
    }
}