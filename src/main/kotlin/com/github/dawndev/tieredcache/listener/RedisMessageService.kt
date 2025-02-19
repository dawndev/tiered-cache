package com.github.dawndev.tieredcache.listener

import com.github.dawndev.tieredcache.core.ICache
import com.github.dawndev.tieredcache.core.MultiLevelCache
import com.github.dawndev.tieredcache.config.RedisPubSubMessage
import com.github.dawndev.tieredcache.constg.RedisMessageEnum
import com.github.dawndev.tieredcache.internal.Parameter
import com.github.dawndev.tieredcache.internal.JsonUtils
import com.github.dawndev.tieredcache.AbstractCacheManager
import com.github.dawndev.tieredcache.redis.RedisDistributedLock
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

/**
 * 拉消息模式
 *
 */
class RedisMessageService(
    private val cacheManager: AbstractCacheManager
) : IMessageService {

    /**
     * 拉消息
     */
    override fun pull() {

        val client = cacheManager.client
        val maxOffset: Long = client.llen(Parameter.getMessageRedisKey()) - 1

        // 没有消息
        if (maxOffset < 0) {
            return
        }

        // 更新本地消息偏移量
        val oldOffset: Long = OFFSET.getAndSet(if (maxOffset > 0) maxOffset else 0)
        if (oldOffset >= maxOffset) {
            return
        }
        val messages = client.lrange(Parameter.getMessageRedisKey(), 0, maxOffset - oldOffset - 1, Parameter.GLOBAL_REDIS_SERIALIZER)
        if (messages.isNullOrEmpty()) {
            return
        }

        // 更新最后一次处理拉消息的时间搓
        updateLastPullTime()

        for (message in messages) {
            if (message == null || message.isBlank()) {
                continue
            }

            val redisPubSubMessage = JsonUtils.decodeFromString<RedisPubSubMessage>(message)
            if (null == redisPubSubMessage) {
                continue
            }

            // 根据缓存名称获取多级缓存，可能有多个
            val caches: Collection<ICache> = cacheManager.getCache(redisPubSubMessage.cacheName)
            for (cache in caches) {
                // 判断缓存是否是多级缓存
                if (cache !is MultiLevelCache)
                    continue

                when (redisPubSubMessage.messageType) {
                    RedisMessageEnum.EVICT -> {
                        if (RedisPubSubMessage.SOURCE == redisPubSubMessage.source) {
                            cache.remoteCache.evict(redisPubSubMessage.key)
                        }

                        // 获取一级缓存，并删除一级缓存数据
                        cache.localCache.evict(redisPubSubMessage.key)
                        logger.info("删除一级缓存 {} 数据,key={}", redisPubSubMessage.cacheName, redisPubSubMessage.key)
                    }
                    RedisMessageEnum.CLEAR -> {
                        if (RedisPubSubMessage.SOURCE == redisPubSubMessage.source) {
                            cache.remoteCache.clear()
                        }

                        // 获取一级缓存，并删除一级缓存数据
                        cache.localCache.clear()
                        logger.info("清除一级缓存 {} 数据", redisPubSubMessage.cacheName)
                    }
                    else -> {
                        logger.error("接收到没有定义的消息数据")
                    }
                }
            }
        }
    }

    /**
     * 清空消息队列
     */
    override fun clearQueue() {

        val lock = RedisDistributedLock(
            cacheManager.client,
            Parameter.getMessageRedisKey(),
            60
        )

        if (lock.lock()) {

            // 清空消息，直接删除key（不可以调换顺序）
            cacheManager.client.delete(Parameter.getMessageRedisKey())
        }

        // 重置偏移量
        OFFSET.getAndSet(-1)
    }

    /**
     * 同步offset
     */
    override fun syncOffset() {
        val maxOffset: Long = cacheManager.client.llen(Parameter.getMessageRedisKey()) - 1
        if (maxOffset < 0) {
            return
        }
        OFFSET.getAndSet(if (maxOffset > 0) maxOffset else 0)
    }

    /**
     * 启动重连pub/sub检查
     */
    override fun reconnection() {
        val time: Long = LAST_PULL_TIME - LAST_PUSH_TIME
        if (time >= RECONNECTION_TIME) {
            try {
                updateLastPushTime()

                //  redis pub/sub 监听器
                RedisMessageListener(cacheManager)
            } catch (e: Exception) {
                logger.error("tiered-cache 清楚一级缓存异常：{}", e.message, e)
            }
        }
    }


    companion object {

        private val logger = LoggerFactory.getLogger(RedisMessageService::class.java)

        // 本地消息偏移量
        val OFFSET = AtomicLong(-1)

        // 最后一次处理推消息的时间搓，忽略并发情况下的误差，只保证可见性即可
        @Volatile
        var LAST_PUSH_TIME = 0L

        // 最后一次处理拉消息的时间搓，忽略并发情况下的误差，只保证可见性即可
        @Volatile
        var LAST_PULL_TIME = 0L

        // pub/sub 重连间隔时间
        val RECONNECTION_TIME = 10 * 1000

        /**
         * 更新最后一次处理拉消息的时间
         */
        fun updateLastPullTime() {
            LAST_PULL_TIME = System.currentTimeMillis()
        }

        /**
         * 更新最后一次处理推消息的时间
         */
        fun updateLastPushTime() {
            LAST_PUSH_TIME = System.currentTimeMillis()
        }
    }
}