package com.github.dawndev.tieredcache.redis.serializer

import com.github.dawndev.tieredcache.exception.SerializationException


/**
 * Redis serializer
 *
 * @author jdg
 */
interface RedisSerializer {
    /**
     * 将给定对象序列化为二进制数据。
     *
     * @param value 需要序列化的对象.允许为 null.
     * @param <T>   T
     * @return 返回对象的二进制数据. 允许为 null.
     * @throws SerializationException 序列化异常
     */
    @Throws(SerializationException::class)
    fun <T> serialize(value: T?): ByteArray?

    /**
     * 将给定的二进制数据中反序列化对象。
     *
     * @param bytes      给定的二进制数据. 允许为 null.
     * @param resultType 返回值类型
     * @param <T>        T
     * @return 反序列化后的对象.允许为 null.
     * @throws SerializationException 序列化异常
     */
    @Throws(SerializationException::class)
    fun <T> deserialize(bytes: ByteArray?, resultType: Class<T>): T?
}