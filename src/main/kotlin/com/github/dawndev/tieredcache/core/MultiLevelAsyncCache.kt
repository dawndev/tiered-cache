package com.github.dawndev.tieredcache.core

import com.github.dawndev.tieredcache.internal.JsonUtils
import com.github.dawndev.tieredcache.internal.fromStoredValue
import com.github.dawndev.tieredcache.internal.taskIfDebug
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor


/**
 * 多级缓存
 *
 * @param localCache           一级缓存
 * @param remoteCache          二级缓存
 * @param name                 缓存名称
 */
class MultiLevelAsyncCache(
    private  val executor: Executor,
    private val localCache: ICache,
    private val remoteCache: ICache,
    private val name: String,
    private val enableNull: Boolean
): IAsyncCache {

    private val logger = LoggerFactory.getLogger(MultiLevelAsyncCache::class.java)

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: String, resultType: Class<T>): CompletableFuture<T?> {
        return CompletableFuture.supplyAsync({
            val localValue = localCache.get(key, resultType)
            logger.taskIfDebug("查询一级缓存。 key={},返回值是:{}", key, JsonUtils.encodeToString(localValue))
            if (localValue != null) {
                localValue.fromStoredValue(enableNull) as T
            } else {
                val remoteValue = remoteCache.get(key, resultType)
                if (remoteValue != null) {
                    localCache.putIfAbsent(key, remoteValue as Any, resultType)
                }
                remoteValue
            }
        }, executor)
    }

    override fun <T> get(key: String, resultType: Class<T>, valueLoader: Callable<T>): CompletableFuture<T?> {
        return CompletableFuture.supplyAsync({
            val localValue = localCache.get(key, resultType)
            logger.taskIfDebug("查询一级缓存。 key={},返回值是:{}", key, JsonUtils.encodeToString(localValue))
            if (localValue != null) {
                localValue
            } else {
                val remoteValue = remoteCache.get(key, resultType, valueLoader)
                if (remoteValue != null) {
                    localCache.putIfAbsent(key, remoteValue as Any, resultType)
                }
                remoteValue
            }
        }, executor)
    }

    override fun put(key: String, value: Any?): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            localCache.put(key, value)
            remoteCache.put(key, value)
        }, executor)
    }

    override fun <T> putIfAbsent(key: String, value: Any?, resultType: Class<T>): CompletableFuture<T?> {
        return CompletableFuture.supplyAsync({
            remoteCache.putIfAbsent(key, value, resultType)
        }, executor)
    }

    override fun evict(key: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            remoteCache.evict(key)
            localCache.evict(key)
        }, executor)
    }

    override fun clear(): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            remoteCache.clear()
            localCache.clear()
        }, executor)
    }

}