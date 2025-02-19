package com.github.dawndev.tieredcache.listener

import com.github.dawndev.tieredcache.config.RedisPubSubMessage
import com.github.dawndev.tieredcache.internal.Parameter
import com.github.dawndev.tieredcache.internal.JsonUtils
import com.github.dawndev.tieredcache.redis.client.RedisTemplate
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object RedisPublisher {

    private val logger = LoggerFactory.getLogger(RedisPublisher::class.java)

    /**
     * 发布消息到频道（Channel）
     *
     * @param redisClient redis客户端
     * @param message     消息内容
     */
    fun publisher(redisClient: RedisTemplate, message: RedisPubSubMessage) {
        publisher(redisClient, message, Parameter.NAMESPACE)
    }

    /**
     * 发布消息到频道（Channel）
     *
     * @param redisClient redis客户端
     * @param message     消息内容
     * @param nameSpace   命名空间
     */
    fun publisher(redisClient: RedisTemplate, message: RedisPubSubMessage, nameSpace: String) {
        val messageJson = JsonUtils.encodeToString(message)

        // pull 拉模式消息
        redisClient.lpush(Parameter.getMessageRedisKey(nameSpace), Parameter.GLOBAL_REDIS_SERIALIZER, messageJson)
        redisClient.expire(Parameter.getMessageRedisKey(nameSpace), 25, TimeUnit.HOURS)

        // pub/sub 推模式消息
        redisClient.publish(Parameter.REDIS_CHANNEL, "m")

        if (logger.isDebugEnabled) {
            logger.debug("redis消息发布者向频道【{}】发布了【{}】消息", Parameter.REDIS_CHANNEL, message.toString())
        }
    }

}
