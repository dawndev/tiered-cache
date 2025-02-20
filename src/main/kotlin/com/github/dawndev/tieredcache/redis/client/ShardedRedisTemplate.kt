package com.github.dawndev.tieredcache.redis.client

import com.github.dawndev.tieredcache.config.RedisConfigure
import com.github.dawndev.tieredcache.exception.RedisClientException
import com.github.dawndev.tieredcache.exception.SerializationException
import com.github.dawndev.tieredcache.internal.NamedThreadFactory
import com.github.dawndev.tieredcache.listener.RedisMessageListener
import com.github.dawndev.tieredcache.redis.cmd.TendisScan
import com.github.dawndev.tieredcache.redis.serializer.RedisSerializer
import com.github.dawndev.tieredcache.redis.serializer.impl.JdkRedisSerializer
import com.github.dawndev.tieredcache.redis.serializer.impl.StringRedisSerializer
import io.lettuce.core.*
import io.lettuce.core.cluster.ClusterClientOptions
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.dynamic.RedisCommandFactory
import io.lettuce.core.internal.HostAndPort
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import java.time.Duration
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val INVALID_NODE_MES = "ERR invalid node"

/**
 * Sharded redis template
 *
 * @param properties
 * @param keySerializer
 * @param valueSerializer
 *
 * @author jdg
 */
class ShardedRedisTemplate(
    private val properties: RedisConfigure,
    override var keySerializer: RedisSerializer = StringRedisSerializer(),
    override var valueSerializer: RedisSerializer = JdkRedisSerializer()
) : RedisTemplate {

    private var client: RedisClusterClient
    private var connection: StatefulRedisClusterConnection<ByteArray, ByteArray>
    private var pubSubConnection: StatefulRedisPubSubConnection<String, String>

    private val executorService: ExecutorService = Executors.newFixedThreadPool(10, NamedThreadFactory("tiered-cache-scan"))

    init {
        val cluster = requireNotNull(properties.cluster)
        val parts = cluster.split("\\,")
        val redisURIs: ArrayList<RedisURI> = ArrayList(parts.size)
        for (part in parts) {
            val hostAndPort = HostAndPort.parse(part)
            val nodeUri = RedisURI.create(hostAndPort.getHostText(), if (hostAndPort.hasPort()) hostAndPort.getPort() else 6379)
            nodeUri.timeout = Duration.ofSeconds(properties.timeout.toLong())
            if (properties.password.isNotBlank()) {
                nodeUri.setPassword(properties.password)
            }
            redisURIs.add(nodeUri)
        }

        this.client = RedisClusterClient.create(redisURIs)
        this.client.setOptions(ClusterClientOptions.builder()
            .autoReconnect(true)
            .pingBeforeActivateConnection(true)
            .build())

        this.client.connect(ByteArrayCodec())
        connection = this.client.connect(ByteArrayCodec())
        pubSubConnection = this.client.connectPubSub()
    }

    override fun <T> get(key: String, resultType: Class<T>): T? {
        return try {
            val sync: RedisClusterCommands<ByteArray, ByteArray> = connection.sync()
            valueSerializer.deserialize(sync[keySerializer.serialize(key)], resultType)
        } catch (e: SerializationException) {
            throw e
        } catch (e: Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun <T> get(key: String, resultType: Class<T>, valueRedisSerializer: RedisSerializer): T? {
        return try {
            val sync: RedisClusterCommands<ByteArray, ByteArray> = connection.sync()
            valueRedisSerializer.deserialize(sync[keySerializer.serialize(key)], resultType)
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun set(key: String, value: Any) {
        try {
            val sync: RedisClusterCommands<ByteArray, ByteArray> = connection.sync()
            sync.set(keySerializer.serialize(key), valueSerializer.serialize(value))
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun set(key: String, value: Any, time: Long, unit: TimeUnit) {
        try {
            val sync: RedisClusterCommands<ByteArray, ByteArray> = connection.sync()
            sync.setex(keySerializer.serialize(key), unit.toSeconds(time), valueSerializer.serialize(value))
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun set(key: String, value: Any, time: Long, unit: TimeUnit, valueRedisSerializer: RedisSerializer) {
        try {
            val sync: RedisClusterCommands<ByteArray, ByteArray?> = connection.sync()
            sync.setex(keySerializer.serialize(key), unit.toSeconds(time), valueRedisSerializer.serialize(value))
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun setNxEx(key: String, value: Any, time: Long): String {
        return try {
            val sync: RedisClusterCommands<ByteArray, ByteArray> = connection.sync()
            sync.set(keySerializer.serialize(key), valueSerializer.serialize(value), SetArgs.Builder.nx().ex(time))
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun delete(vararg keys: String): Long {
        return if (keys.isEmpty()) {
            0L
        } else try {
            val sync: RedisClusterCommands<ByteArray, ByteArray> = connection.sync()
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
            val sync: RedisClusterCommands<ByteArray, ByteArray> = connection.sync()
            sync.exists(keySerializer.serialize(key)) > 0
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun expire(key: String, timeout: Long, timeUnit: TimeUnit) {
        try {
            val sync: RedisClusterCommands<ByteArray, ByteArray> = connection.sync()
            sync.expire(keySerializer.serialize(key), timeUnit.toSeconds(timeout))
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun getExpire(key: String): Long {
        return try {
            val sync: RedisClusterCommands<ByteArray, ByteArray> = connection.sync()
            sync.ttl(keySerializer.serialize(key))
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun scan(pattern: String): Set<String> {
        val keys = Collections.synchronizedSet(HashSet<String>())

        // 腾讯云版本redis
        if (tencentRedis) {
            try {
                val sync: RedisClusterCommands<ByteArray, ByteArray> = connection.sync()
                val nodeStr = sync.clusterNodes()
                val nodes = nodeStr.split("\n").toTypedArray()
                val countDownLatch = CountDownLatch(nodes.size)
                for (node in nodes) {
                    if (!node.contains("master")) {
                        countDownLatch.countDown()
                        continue
                    }
                    executorService.submit {
                        try {
                            val innerKeys: MutableList<String> = LinkedList()
                            var cursor = 0L
                            do {
                                // 2150b1d23fc132cb6ff5a9553f5f1af9f19b0cc2 127.0.0.1:6379@13357 master - 0 1600342826089 2 connected 10923-16383
                                val nodeId = node.split(" ").toTypedArray()[0]
                                val factory = RedisCommandFactory(connection)
                                val commands: TendisScan = factory.getCommands(TendisScan::class.java)
                                val objects = commands.scan(cursor, pattern, 10000, nodeId)
                                if (objects.isNullOrEmpty()) {
                                    break
                                }

                                // 更新游标位
                                cursor = (objects.first() as String).toLong()

                                // 暂存key
                                if (objects.size == 2) {
                                    innerKeys.addAll((objects[1] as ArrayList<out String>))
                                }
                            } while (cursor != 0L)

                            if (!innerKeys.isNullOrEmpty()) {
                                keys.addAll(innerKeys)
                            }
                        } finally {
                            countDownLatch.countDown()
                        }
                    }
                }
                countDownLatch.await(10, TimeUnit.MINUTES)
            } catch (e: SerializationException) {
                throw e
            } catch (e: java.lang.Exception) {
                throw RedisClientException(e.message, e)
            }
            return keys
        }

        // 普通redis
        try {
            this.client.connect(ByteArrayCodec())
            val scan = ScanIterator.scan(
                connection.sync(), ScanArgs.Builder.limit(10000).match(pattern)
            )
            while (scan.hasNext()) {
                val next = keySerializer.deserialize(scan.next(), String::class.java)
                if (next != null) {
                    keys.add(next)
                }
            }
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            if (INVALID_NODE_MES == e.message) {
                tencentRedis = true
                return scan(pattern)
            }
            throw RedisClientException(e.message, e)
        }
        return keys
    }

    override fun lpush(key: String, valueRedisSerializer: RedisSerializer, vararg values: String): Long {
        return try {
            if (values.isEmpty()) {
                return 0L
            }
            val sync: RedisClusterCommands<ByteArray, ByteArray> = connection.sync()
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

    override fun lrange(key: String, start: Long, end: Long, valueRedisSerializer: RedisSerializer): List<String> {
        return try {
            val sync: RedisClusterCommands<ByteArray, ByteArray?> = connection.sync()
            val list: MutableList<String> = ArrayList()
            val values = sync.lrange(keySerializer.serialize(key), start, end)
            if (values.isNullOrEmpty()) {
                return list
            }
            for (value in values) {
                Optional.ofNullable(valueRedisSerializer.deserialize(value, String::class.java)).ifPresent {
                    list.add(it)
                }
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
            val sync: RedisClusterCommands<ByteArray, ByteArray> = connection.sync()
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
            val sync = pubSubConnection.sync()
            sync.publish(channel, message)
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    override fun subscribe(messageListener: RedisMessageListener, vararg channel: String) {
        try {
            val connection: StatefulRedisPubSubConnection<String, String> = client.connectPubSub()
            connection.sync().subscribe(*channel)
            connection.addListener(messageListener)
        } catch (e: SerializationException) {
            throw e
        } catch (e: java.lang.Exception) {
            throw RedisClientException(e.message, e)
        }
    }

    companion object {
        @Volatile
        private var tencentRedis = false
    }
}