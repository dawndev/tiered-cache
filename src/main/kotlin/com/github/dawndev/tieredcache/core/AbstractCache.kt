package com.github.dawndev.tieredcache.core

import com.github.dawndev.tieredcache.config.RedisPubSubMessage
import com.github.dawndev.tieredcache.constg.RedisMessageEnum
import com.github.dawndev.tieredcache.internal.NullValue
import com.github.dawndev.tieredcache.listener.RedisPublisher
import com.github.dawndev.tieredcache.metrics.CacheMetrics
import com.github.dawndev.tieredcache.redis.client.RedisTemplate
import io.micrometer.core.instrument.simple.SimpleMeterRegistry


/**
 * Cache 接口的抽象实现类，对公共的方法做了一写实现，如是否允许存NULL值
 * <p>
 *  如果允许为NULL值，则需要在内部将NULL替换成{@link NullValue#INSTANCE} 对象
 *
 * @param name              缓存名称
 * @param enableNull        获取是否允许存在NULL值
 *
 * @author Espresso
 */
abstract class AbstractCache(
    override val name: String,
    open val enableNull: Boolean
): ICache {

    // 缓存指标
    val metrics: CacheMetrics? by lazy {
        CacheMetrics(SimpleMeterRegistry(), name)
    }


    /**
     * Convert the given value from the internal store to a user value
     * returned from the get method (adapting {@code null}).
     *
     * @param storeValue the store value
     * @return the value to return to the user
     */
    protected open fun fromStoreValue(storeValue: Any?): Any? {
        return if (enableNull && storeValue is NullValue) {
            null
        } else storeValue
    }

    /**
     * Convert the given user value, as passed into the put method,
     * to a value in the internal store (adapting `null`).
     *
     * @param userValue the given user value
     * @return the value to store
     */
    protected open fun toStoreValue(userValue: Any?): Any? {
        return if (enableNull && userValue == null) {
            NullValue
        } else userValue
    }

    fun deleteLocalCache(key: String, redisClient: RedisTemplate) {
        // 删除一级缓存需要用到redis的Pub/Sub（订阅/发布）模式，否则集群中其他服服务器节点的一级缓存数据无法删除
        val message = RedisPubSubMessage(
            name,
            key,
            RedisMessageEnum.EVICT
        )
        // 发布消息
        RedisPublisher.publisher(redisClient, message)
    }
}