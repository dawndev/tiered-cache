package com.github.dawndev.tieredcache.core.remote

import com.github.dawndev.tieredcache.config.RemoteCacheOptions
import com.github.dawndev.tieredcache.core.AbstractRemoteCache
import com.github.dawndev.tieredcache.core.ICache
import com.github.dawndev.tieredcache.exception.CacheLoadException
import com.github.dawndev.tieredcache.internal.*
import com.github.dawndev.tieredcache.internal.JsonUtils
import com.github.dawndev.tieredcache.internal.Parameter
import com.github.dawndev.tieredcache.redis.RedisDistributedLock
import com.github.dawndev.tieredcache.redis.client.RedisTemplate
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

/**
 * 基于Redis实现的二级缓存

 * @param name            缓存名称
 * @param redisClient     redis客户端 [RedisTemplate]
 * @param expiration      key的有效时间
 * @param preloadTime     缓存主动在失效前强制刷新缓存的时间
 * @param enableForceRefresh    是否强制刷新（执行被缓存的方法），默认是false
 * @param enablePrefix       是否使用缓存名称作为前缀
 * @param enableNull 是否允许存NULL值，模式允许
 * @param magnification   非空值和null值之间的时间倍率
 *
 * @author Espresso
 */
@Suppress("UNCHECKED_CAST")
open class RedisRemoteCache(
    override val name: String,
    override val enableNull: Boolean,
    private val redisClient: RedisTemplate,
    private val expiration: Long,
    private val preloadTime: Long,
    private val enableForceRefresh: Boolean,
    private val enablePrefix: Boolean,
    private val magnification: Int
) : AbstractRemoteCache<RedisCacheKey>(name, enableNull, preloadTime, enableForceRefresh, magnification), ICache {

    /**
     * @param name                  缓存名称
     * @param redisClient           redis客户端 redis 客户端
     * @param options               二级缓存配置[RemoteCacheOptions]
     */
    constructor(
        name: String,
        redisClient: RedisTemplate,
        options: RemoteCacheOptions,
    ) : this(
        name,
        options.enableNull,
        redisClient,
        options.expiration,
        options.preloadTime,
        options.enableForceRefresh,
        options.enablePrefix,
        options.magnification
    )

    private val logger = LoggerFactory.getLogger(RedisRemoteCache::class.java)

    /**
     * 等待线程容器
     */
    private val container: AwaitThreadContainer = AwaitThreadContainer()

    override val nativeRef: Any
        get() = redisClient

    override fun <T> get(key: String, resultType: Class<T>): T? {

        val redisCacheKey = getRedisCacheKey(key)
        logger.debug("redis缓存 key= {} 查询redis缓存", redisCacheKey.getKey())
        return redisClient.get(redisCacheKey.getKey(), resultType)
    }

    override fun <T> get(key: String, resultType: Class<T>, valueLoader: Callable<T>): T? {


        val redisCacheKey = this.getRedisCacheKey(key)
        logger.taskIfDebug("redis缓存 key= {} 查询redis缓存如果没有命中，从数据库获取数据", redisCacheKey.getKey())


        // 先获取缓存，如果有直接返回
        val result = redisClient.get(redisCacheKey.getKey(), resultType)
        if (result != null || redisClient.hasKey(redisCacheKey.getKey())) {
            // 刷新缓存
            super.refreshCache(redisCacheKey, resultType, valueLoader, result)
            return super.fromStoreValue(result as Any) as T
        }

        // 执行缓存方法
        return this.executeCacheMethod(redisCacheKey, resultType, valueLoader)
    }

    override fun put(key: String, value: Any?) {
        val redisCacheKey = this.getRedisCacheKey(key)
        logger.taskIfDebug("redis缓存 key= {} put缓存，缓存值：{}", redisCacheKey.getKey(), JsonUtils.toJSONString(value))
        putValue(redisCacheKey, value)
    }

    override fun <T> putIfAbsent(key: String, value: Any?, resultType: Class<T>): T? {
        logger.taskIfDebug("redis缓存 key= {} putIfAbsent缓存，缓存值：{}", this.getRedisCacheKey(key).getKey(), JsonUtils.toJSONString(value))
        val result = get(key, resultType)
        if (result != null) {
            return result
        }
        put(key, value)
        return null
    }

    override fun evict(key: String) {
        val redisCacheKey = this.getRedisCacheKey(key)
        logger.info("清除redis缓存 key= {} ", redisCacheKey.getKey())
        redisClient.delete(redisCacheKey.getKey())
    }

    override fun clear() {
        // 必须开启了使用缓存名称作为前缀，clear才有效
        if (enablePrefix) {
            logger.info("清空redis缓存 ，缓存前缀为{}", name)
            val keys = redisClient.scan("$name*")
            if (!keys.isNullOrEmpty()) {
                redisClient.delete(keys)
            }
        }
    }

    /**
     * 获取 RedisCacheKey
     *
     * @param key 缓存key
     * @return RedisCacheKey
     */
    private fun getRedisCacheKey(key: String): RedisCacheKey {
        return RedisCacheKey.build {
            this@build.keyElement = key
            this@build.serializer = redisClient.keySerializer
            this@build.cacheName = name
            this@build.enablePrefix = this@RedisRemoteCache.enablePrefix

        }
    }


    /**
     * 获取锁的线程等待500ms,如果500ms都没返回，则直接释放锁放下一个请求进来，防止第一个线程异常挂掉
     */
    private fun <T> executeCacheMethod(redisCacheKey: RedisCacheKey, resultType: Class<T>, valueLoader: Callable<T>): T? {
        val redisLock = RedisDistributedLock(
            redisClient,
            Parameter.getRedisLockKey(redisCacheKey.getKey()),
            1
        )

        while (true) {
            try {
                // 先取缓存，如果有直接返回，没有再去做拿锁操作
                val result = redisClient.get(redisCacheKey.getKey(), resultType)
                if (result != null) {
                    logger.taskIfDebug("redis缓存 key= {} 获取到锁后查询查询缓存命中，不需要执行被缓存的方法", redisCacheKey.getKey())

                    return fromStoreValue(result) as T?
                }

                // 获取分布式锁去后台查询数据
                if (redisLock.lock()) {
                    val t = loaderAndPutValue(redisCacheKey, valueLoader)
                    logger.taskIfDebug("redis缓存 key= {} 从数据库获取数据完毕，唤醒所有等待线程", redisCacheKey.getKey())

                    // 唤醒线程
                    container.signalAll(redisCacheKey.getKey())
                    return t
                }
                // 线程等待
                logger.taskIfDebug("redis缓存 key= {} 从数据库获取数据未获取到锁，进入等待状态，等待{}毫秒", redisCacheKey.getKey(), Parameter.WAIT_TIME)
                container.await(redisCacheKey.getKey(), Parameter.WAIT_TIME)
            } catch (e: java.lang.Exception) {
                container.signalAll(redisCacheKey.getKey())
                throw CacheLoadException(redisCacheKey.getKey(), e)
            } finally {
                redisLock.unlock()
            }
        }
    }

    /**
     * 加载并将数据放到redis缓存
     */
    private fun <T> loaderAndPutValue(key: RedisCacheKey, valueLoader: Callable<T>): T? {
        val start = System.currentTimeMillis()

        return try {
            // 加载数据
            val result = this.putValue(key, valueLoader.call() as Any)
            logger.taskIfDebug("redis缓存 key={} 执行被缓存的方法，并将其放入缓存, 耗时：{}。数据:{}", key.getKey(), System.currentTimeMillis() - start, JsonUtils.toJSONString(result))
            super.fromStoreValue(result) as T
        } catch (e: java.lang.Exception) {
            throw CacheLoadException(key.getKey(), e)
        }
    }

    private fun putValue(key: RedisCacheKey, value: Any?): Any? {
        val result = super.toStoreValue(value)
        // redis 缓存不允许直接存NULL，如果结果返回NULL需要删除缓存
        if (result == null) {
            redisClient.delete(key.getKey())
            return result
        }
        // 不允许缓存NULL值，删除缓存
        if (!enableNull && result is NullValue) {
            redisClient.delete(key.getKey())
            return result
        }

        // 允许缓存NULL值
        var expirationTime = expiration
        // 允许缓存NULL值且缓存为值为null时需要重新计算缓存时间
        if (enableNull && result is NullValue) {
            expirationTime /= magnification
        }
        // 将数据放到缓存
        redisClient.set(key.getKey(), result, expirationTime, TimeUnit.MILLISECONDS)
        return result
    }

    /**
     * 软刷新，直接修改缓存时间
     *
     * @param remoteKey [RedisCacheKey]
     */
    override fun softRefresh(remoteKey: RedisCacheKey) {
        // 加一个分布式锁，只放一个请求去刷新缓存
        val lockKey = Parameter.getRedisLockKey(remoteKey.getKey())
        val redisLock = RedisDistributedLock(redisClient, lockKey)
        try {
            if (redisLock.tryLock()) {
                redisClient.expire(remoteKey.getKey(), expiration, TimeUnit.MILLISECONDS)
            }
        } catch (e: Exception) {
            logger.error(e.message, e)
        } finally {
            redisLock.unlock()
        }
    }

    /**
     * 硬刷新（执行被缓存的方法）
     *
     * @param redisCacheKey [RedisCacheKey]
     * @param valueLoader   数据加载器
     * @param preloadTime   缓存预加载时间
     */
    override fun <T> forceRefresh(
        redisCacheKey: RedisCacheKey,
        resultType: Class<T>,
        valueLoader: Callable<T>,
        preloadTime: Long
    ) {
        // 尽量少的去开启线程，因为线程池是有限的 todo
       // GlobalScope.launch { // 创建一个新协程
            // 加一个分布式锁，只放一个请求去刷新缓存
            val redisLock = RedisDistributedLock(redisClient, redisCacheKey.getKey() + "_lock")
            try {
                if (redisLock.lock()) {
                    // 获取锁之后再判断一下过期时间，看是否需要加载数据
                    if (isRefresh(redisCacheKey, preloadTime)) {
                        // 获取缓存中老数据
                        val oldDate = redisClient.get(redisCacheKey.getKey(), resultType)
                        // 加载数据并放到缓存
                        val newDate = loaderAndPutValue(redisCacheKey, valueLoader)
                        // 比较新老数据是否相等，如果不想等就删除一级缓存
                        if (oldDate != newDate && JsonUtils.toJSONString(oldDate) != JsonUtils.toJSONString(newDate)) {
                            logger.debug("二级缓存数据发生变更，同步刷新一级缓存")
                            deleteLocalCache((redisCacheKey.keyElement as String), redisClient)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(e.message, e)
            } finally {
                redisLock.unlock()
            }
        //}

        // 尽量少的去开启线程，因为线程池是有限的 todo
        // ThreadTaskUtils.run {}
    }

    /**
     * 判断是否需要刷新缓存
     *
     * @param remoteKey 缓存key
     * @param preloadTime   预加载时间（经过计算后的时间）
     * @return boolean
     */
    override fun isRefresh(remoteKey: RedisCacheKey, preloadTime: Long): Boolean {
        // 获取锁之后再判断一下过期时间，看是否需要加载数据
        val ttl = redisClient.getExpire(remoteKey.getKey())
        // -2表示key不存在
        return if (ttl == -2L) {
            true
        } else {
            // 当前缓存时间小于刷新时间就需要刷新缓存
            ttl > 0 && TimeUnit.SECONDS.toMillis(ttl) <= preloadTime
        }

    }
}