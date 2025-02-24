package com.github.dawndev.tieredcache.core

import com.github.dawndev.tieredcache.config.MultiCacheOptions
import com.github.dawndev.tieredcache.config.RedisPubSubMessage
import com.github.dawndev.tieredcache.constg.RedisMessageEnum
import com.github.dawndev.tieredcache.internal.JsonUtils
import com.github.dawndev.tieredcache.internal.fromStoredValue
import com.github.dawndev.tieredcache.internal.taskIfDebug
import com.github.dawndev.tieredcache.listener.RedisPublisher
import com.github.dawndev.tieredcache.redis.client.RedisTemplate
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.Callable


/**
 * 多级缓存
 *
 * @param client               redis客户端 [RedisTemplate]
 * @param localCache           一级缓存
 * @param remoteCache          二级缓存
 * @param enableLocalCache     是否使用一级缓存，默认是
 * @param name                 缓存名称
 * @param multiCacheSetting    多级缓存配置
 */
open class MultiLevelCache(
    private val client: RedisTemplate,
    open val localCache: ICache,
    open val remoteCache: ICache,
    private val enableLocalCache: Boolean,
    override val name: String,
    private val multiCacheSetting: MultiCacheOptions,
    override val enableNull: Boolean
) : AbstractCache(name), ICache {

    constructor(
        client: RedisTemplate,
        localCache: ICache,
        remoteCache: ICache,
        multiCacheSetting: MultiCacheOptions,
    ) : this(
        client,
        localCache,
        remoteCache,
        multiCacheSetting.enableLocal,
        remoteCache.name,
        multiCacheSetting,
        multiCacheSetting.enableNull
    )

    private val logger = LoggerFactory.getLogger(MultiLevelCache::class.java)

    override val nativeRef: Any
        get() = this

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: String, resultType: Class<T>): T? {
        if (enableLocalCache) {
            val result = localCache.get(key, resultType)
            logger.taskIfDebug("查询一级缓存。 key={},返回值是:{}", key, JsonUtils.encodeToString(result))

            if (result != null) {
                return result.fromStoredValue(enableNull) as T
            }
        }

        val result = remoteCache.get(key, resultType)
        if (enableLocalCache) {
            localCache.putIfAbsent(key, result as Any, resultType)
        }
        logger.taskIfDebug("查询二级缓存,并将数据放到一级缓存。 key={},返回值是:{}", key, JsonUtils.encodeToString(result))

        return result
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: String, resultType: Class<T>, valueLoader: Callable<T>): T? {
        if (enableLocalCache) {
            val result = localCache.get(key, resultType)
            logger.taskIfDebug("查询一级缓存。 key={},返回值是:{}", key, JsonUtils.encodeToString(result))

            if (result != null) {
                return result.fromStoredValue(enableNull) as T
            }
        }
        val result = remoteCache.get(key, resultType, valueLoader)

        if (enableLocalCache) {
            localCache.putIfAbsent(key, result as Any, resultType)
        }
        val list = LinkedList<Int>()
        // { 1, 3, 4, 5}
        list.last {
            it > 4
        }
        logger.taskIfDebug("查询二级缓存,并将数据放到一级缓存。 key={},返回值是:{}", key, JsonUtils.encodeToString(result))
        return result
    }

    override fun put(key: String, value: Any?) {
        remoteCache.put(key, value)

        // 删除一级缓存
        if (enableLocalCache) {
            deleteLocalCache(key, client)
        }
    }

    override fun <T> putIfAbsent(key: String, value: Any?, resultType: Class<T>): T? {
        val result = remoteCache.putIfAbsent(key, value, resultType)

        // 删除一级缓存
        if (enableLocalCache) {
            deleteLocalCache(key, client)
        }
        return result
    }

    override fun evict(key: String) {

        // 删除的时候要先删除二级缓存再删除一级缓存，否则有并发问题
        remoteCache.evict(key)

        // 删除一级缓存
        if (enableLocalCache) {
            deleteLocalCache(key, client)
        }
    }

    override fun clear() {

        // 删除的时候要先删除二级缓存再删除一级缓存，否则有并发问题
        remoteCache.clear()
        if (enableLocalCache) {

            // 清除一级缓存需要用到redis的订阅/发布模式，否则集群中其他服服务器节点的一级缓存数据无法删除
            val message = RedisPubSubMessage(
                name, "", RedisMessageEnum.CLEAR
            )

            // 发布消息
            RedisPublisher.publisher(client, message)
        }
    }
}