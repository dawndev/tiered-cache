package com.github.dawndev.tieredcache.core

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor


/**
 * 多级缓存(异步)
 */
class MultiLevelAsyncCache(
    private  val executor: Executor,
    private val cache: MultiLevelCache
): IAsyncCache {

    override fun <T> get(key: String, resultType: Class<T>): CompletableFuture<T?> {
        return CompletableFuture.supplyAsync({
            cache.get(key, resultType)
        }, executor)
    }

    override fun <T> get(key: String, resultType: Class<T>, valueLoader: Callable<T>): CompletableFuture<T?> {
        return CompletableFuture.supplyAsync({
            cache.get(key, resultType, valueLoader)
        }, executor)
    }

    override fun put(key: String, value: Any?): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            cache.put(key, value)
        }, executor)
    }

    override fun <T> putIfAbsent(key: String, value: Any?, resultType: Class<T>): CompletableFuture<T?> {
        return CompletableFuture.supplyAsync({
            cache.putIfAbsent(key, value, resultType)
        }, executor)
    }

    override fun evict(key: String): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            cache.evict(key)
        }, executor)
    }

    override fun clear(): CompletableFuture<Void> {
        return CompletableFuture.runAsync({
            cache.clear()
        }, executor)
    }

}