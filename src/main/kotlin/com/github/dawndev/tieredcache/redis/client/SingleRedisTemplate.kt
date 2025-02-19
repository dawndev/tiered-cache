package com.github.dawndev.tieredcache.redis.client

import com.github.dawndev.tieredcache.config.RedisConfigure
import com.github.dawndev.tieredcache.exception.RedisClientException
import com.github.dawndev.tieredcache.listener.RedisMessageListener
import com.github.dawndev.tieredcache.redis.serializer.RedisSerializer
import com.github.dawndev.tieredcache.redis.serializer.impl.JdkRedisSerializer
import com.github.dawndev.tieredcache.redis.serializer.impl.StringRedisSerializer
import io.lettuce.core.*
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import kotlinx.serialization.SerializationException
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * Single redis template
 * 单机版Redis客户端
 *
 * @param properties
 * @param keySerializer
 * @param valueSerializer
 *
 * @author jdg
 */
class SingleRedisTemplate(
    private val properties: RedisConfigure,
    override var keySerializer: RedisSerializer = StringRedisSerializer(),
    override var valueSerializer: RedisSerializer = JdkRedisSerializer()
) : RedisTemplate {

    private var client: RedisClient
    private var connection: StatefulRedisConnection<ByteArray, ByteArray>
    private var pubSubConnection: StatefulRedisPubSubConnection<String, String>

    init {
        val redisURI = RedisURI.builder().withHost(properties.host)
            .withDatabase(properties.database)
            .withPort(properties.port)
            .withTimeout(Duration.ofSeconds(properties.timeout.toLong()))
            .build()
        if (properties.password.isNotBlank()) {
            redisURI.setPassword(properties.password)
        }

        client = RedisClient.create(redisURI)
        client.options = ClientOptions.builder()
            .autoReconnect(true)
            .pingBeforeActivateConnection(true)
            .build()

        this.connection = client.connect(ByteArrayCodec())
        this.pubSubConnection = client.connectPubSub()
    }

    override fun <T> get(key: String, resultType: Class<T>): T? {
        return try {
            val sync = connection.sync()
            valueSerializer.deserialize(sync[keySerializer.serialize(key)], resultType)
        } catch (e: SerializationException) {
            throw e
        } catch (e: Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun <T> get(key: String, resultType: Class<T>, valueRedisSerializer: RedisSerializer): T? {
        return try {
            val sync = connection.sync()
            valueRedisSerializer.deserialize(sync[keySerializer.serialize(key)], resultType)
        } catch (e: SerializationException) {
            throw e
        } catch (e: Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun set(key: String, value: Any) {
        try {
            val sync = connection.sync()
            sync.set(keySerializer.serialize(key), valueSerializer.serialize(value))
        } catch (e: SerializationException) {
            throw e
        } catch (e: Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun set(key: String, value: Any, time: Long, unit: TimeUnit) {
        try {
            val sync = connection.sync()
            sync.setex(keySerializer.serialize(key), unit.toSeconds(time), valueSerializer.serialize(value))
        } catch (e: SerializationException) {
            throw e
        } catch (e: Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun set(key: String, value: Any, time: Long, unit: TimeUnit, valueRedisSerializer: RedisSerializer) {
        try {
            val sync = connection.sync()
            sync.setex(keySerializer.serialize(key), unit.toSeconds(time), valueRedisSerializer.serialize(value))
        } catch (e: SerializationException) {
            throw e
        } catch (e: Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun setNxEx(key: String, value: Any, time: Long): String {
        return try {
            val sync = connection.sync()
            sync.set(keySerializer.serialize(key), valueSerializer.serialize(value), SetArgs.Builder.nx().ex(time))
        } catch (e: SerializationException) {
            throw e
        } catch (e: Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun delete(vararg keys: String): Long {
        return if (keys.isEmpty()) {
            0L
        } else try {
            val sync = connection.sync()
            val bkeys = arrayOfNulls<ByteArray>(keys.size)
            for (i in keys.indices) {
                bkeys[i] = keySerializer.serialize(keys[i])
            }
            sync.del(*bkeys)
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun delete(keys: Set<String>): Long {
        return delete(*keys.toTypedArray())
    }

    override fun hasKey(key: String): Boolean {
        return try {
            val sync = connection.sync()
            sync.exists(keySerializer.serialize(key)) > 0
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun expire(key: String, timeout: Long, timeUnit: TimeUnit) {
        try {
            val sync = connection.sync()
            sync.expire(keySerializer.serialize(key), timeUnit.toSeconds(timeout))
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun getExpire(key: String): Long {
        return try {
            val sync = connection.sync()
            sync.ttl(keySerializer.serialize(key))
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun scan(pattern: String): Set<String> {
        val keys: HashSet<String> = HashSet()
        try {
            val sync = connection.sync()
            var finished: Boolean
            var cursor = ScanCursor.INITIAL
            do {
                val scanCursor = sync.scan(
                    cursor,
                    ScanArgs.Builder.limit(10000).match(pattern)
                )
                scanCursor.keys.forEach { key ->
                    Optional.ofNullable(keySerializer.deserialize(key, String::class.java)).ifPresent {
                        keys.add(it)
                    }
                }

                finished = scanCursor.isFinished
                cursor = ScanCursor.of(scanCursor.cursor)
            } while (!finished)
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
        return keys

    }

    override fun lpush(key: String, valueRedisSerializer: RedisSerializer, vararg values: String): Long {
        return if (values.isEmpty()) {
            0L
        } else try {
            val sync = connection.sync()
            val bvalues = arrayOfNulls<ByteArray>(values.size)
            for (i in values.indices) {
                bvalues[i] = valueRedisSerializer.serialize(values[i])
            }
            sync.lpush(keySerializer.serialize(key), *bvalues)
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun llen(key: String): Long {
        return try {
            val sync = connection.sync()
            sync.llen(keySerializer.serialize(key))
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun lrange(key: String, start: Long, end: Long, valueRedisSerializer: RedisSerializer): List<String?> {
        return try {
            val sync = connection.sync()
            val list: MutableList<String?> = ArrayList()
            val values = sync.lrange(keySerializer.serialize(key), start, end)
            if (values.isNullOrEmpty()) {
                return list
            }
            for (value in values) {
                list.add(valueRedisSerializer.deserialize(value, String::class.java))
            }
            list
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun eval(script: String, keys: List<String>, args: List<String>): Any {
        return try {
            val sync = connection.sync()
            val keyList = ArrayList<ByteArray?>()
            val argList = ArrayList<ByteArray?>()
            keys.stream().forEach { key ->
                keyList.add(keySerializer.serialize(key))
            }
            args.stream().forEach { arg ->
                argList.add(valueSerializer.serialize(arg))
            }
            sync.eval(
                script,
                ScriptOutputType.INTEGER,
                keyList.toArray(Array(0) { ByteArray(0) }),
                *argList.toArray(Array(0) { ByteArray(0) })
            )
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }

    }

    override fun publish(channel: String, message: String): Long {
        return try {
            pubSubConnection.sync().publish(channel, message)
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun subscribe(messageListener: RedisMessageListener, vararg channel: String) {
        try {
            val connection = client.connectPubSub()
            connection.sync().subscribe(*channel)
            connection.addListener(messageListener)
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }
}