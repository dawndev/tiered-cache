package com.github.dawndev.tieredcache.core

import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture

interface IAsyncCache {

    /**
     * 根据KEY返回缓存中对应的值，并将其返回类型转换成对应类型，如果对应key不存在返回NULL
     *
     * @param key        缓存key
     * @param resultType 返回值类型
     * @param <T>        Object
     * @return 缓存key对应的值
     */
    fun <T> get(key: String, resultType: Class<T>): CompletableFuture<T?>

    /**
     * 根据KEY返回缓存中对应的值，并将其返回类型转换成对应类型，如果对应key不存在则调用valueLoader加载数据
     *
     * @param key         缓存key
     * @param resultType  返回值类型
     * @param valueLoader 加载缓存的回调方法
     * @param <T>         Object
     * @return 缓存key对应的值
     */
    fun <T> get(key: String, resultType: Class<T>, valueLoader: Callable<T>): CompletableFuture<T?>

    /**
     * 将对应key-value放到缓存，如果key原来有值就直接覆盖
     *
     * @param key   缓存key
     * @param value 缓存的值
     */
    fun put(key: String, value: Any?): CompletableFuture<Void>

    /**
     * 如果缓存key没有对应的值就将值put到缓存，如果有就直接返回原有的值
     *
     * @param value      缓存key对应的值
     * @param resultType 返回值类型
     * @param <T> T
     * @return 因为值本身可能为NULL，或者缓存key本来就没有对应值的时候也为NULL，
     * 所以如果返回NULL就表示已经将key-value键值对放到了缓存中
     * @since 4.1
     */
    fun <T> putIfAbsent(key: String, value: Any?, resultType: Class<T>): CompletableFuture<T?>

    /**
     * 在缓存中删除对应的key invalidate
     *
     * @param key 缓存key
     */
    fun evict(key: String): CompletableFuture<Void>

    /**
     * 清除缓存
     */
    fun clear(): CompletableFuture<Void>
}