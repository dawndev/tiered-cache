package com.github.dawndev.tieredcache.redis.serializer

import com.github.dawndev.tieredcache.internal.NullValue
import java.util.*

/**
 * 序列化方式的抽象实现
 *
 * @author jdg
 */
abstract class AbstractRedisSerializer : RedisSerializer {


    private var nullValueBytes: ByteArray? = null

    /**
     * 获取空值的序列化值
     *
     * @return byte[]
     */
    open fun getNullValueBytes(): ByteArray? {
        if (Objects.isNull(nullValueBytes)) {
            synchronized(this) { nullValueBytes = serialize(NullValue) }
        }
        return nullValueBytes
    }

    /**
     * is empty
     * @param data ByteArray?
     * @return Boolean
     */
    fun isEmpty(data: ByteArray?): Boolean {
        return data == null || data.isEmpty()
    }

    companion object {
        val EMPTY_ARRAY = ByteArray(0)
    }
}