package com.github.dawndev.tieredcache.listener

import com.github.dawndev.tieredcache.internal.Parameter
import com.github.dawndev.tieredcache.AbstractCacheManager
import io.lettuce.core.pubsub.RedisPubSubListener
import org.slf4j.LoggerFactory

/**
 * redis消息的订阅者
 *
 * @author jdg
 */
class RedisMessageListener(
    cacheManager: AbstractCacheManager
) : RedisPubSubListener<String, String> {

    private var redisMessageService: IMessageService

    init {
        // 创建监听
        cacheManager.client.subscribe(this, Parameter.REDIS_CHANNEL)
        redisMessageService = RedisMessageService(cacheManager)
    }


    override fun message(channel: String, message: String) {
        try {

            // 更新最后一次处理拉消息的时间
            RedisMessageService.updateLastPushTime()

            redisMessageService.pull()
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error("tiered-cache | clear remote cache：{}", e.message, e)
        }
    }

    override fun message(pattern: String?, channel: String?, message: String?) {
        //pass
    }

    override fun subscribed(channel: String?, count: Long) {
        //pass
    }

    override fun psubscribed(pattern: String?, count: Long) {
        //pass
    }

    override fun unsubscribed(channel: String?, count: Long) {
        //pass
    }

    override fun punsubscribed(pattern: String?, count: Long) {
        //pass
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RedisMessageListener::class.java)
    }
}