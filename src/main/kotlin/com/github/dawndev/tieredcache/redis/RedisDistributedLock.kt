package com.github.dawndev.tieredcache.redis

import com.github.dawndev.tieredcache.constg.*
import com.github.dawndev.tieredcache.redis.client.RedisTemplate
import java.util.*

/**
 * Redis分布式锁
 * 使用 SET resource-name anystring NX EX max-lock-time 实现
 * <p>
 * 该方案在 Redis 官方 SET 命令页有详细介绍。
 * http://doc.redisfans.com/string/set.html
 * <p>
 * 在介绍该分布式锁设计之前，我们先来看一下在从 Redis 2.6.12 开始 SET 提供的新特性，
 * 命令 SET key value [EX seconds] [PX milliseconds] [NX|XX]，其中：
 * <p>
 * EX seconds — 以秒为单位设置 key 的过期时间；
 * PX milliseconds — 以毫秒为单位设置 key 的过期时间；
 * NX — 将key 的值设为value ，当且仅当key 不存在，等效于 SETNX。
 * XX — 将key 的值设为value ，当且仅当key 存在，等效于 SETEX。
 * <p>
 * 命令 SET resource-name anystring NX EX max-lock-time 是一种在 Redis 中实现锁的简单方法。
 * <p>
 * 客户端执行以上的命令：
 * <p>
 * 如果服务器返回 OK ，那么这个客户端获得锁。
 * 如果服务器返回 NIL ，那么客户端获取锁失败，可以在稍后再重试。
 *
 * @author jdg
 * @version 1.0
 * @since 2021/01/12
 */
class RedisDistributedLock(
    private val client: RedisTemplate,          // redis客户端
    private val lockKey: String,        // 锁的key（Redis的Key）
    private var expireTime: Int = 0,    // 锁的过期时间
    private var timeOut: Long = 0       // 请求锁的超时时间(单位：毫秒)
) {

    private val random = Random()

    @Volatile
    var locked = false // 锁标记

    private var lockValue: String = ""

    constructor(
        client: RedisTemplate,
        lockKey: String,
        expireTime: Int
    ) : this(client, lockKey, expireTime, 0)

    constructor(
        client: RedisTemplate,
        lockKey: String,
        timeOut: Long
    ) : this(client, lockKey, 0, timeOut)

    /**
     * 尝试获取锁 超时返回
     *
     * @return boolean
     */
    fun tryLock(): Boolean {

        // 生成随机key
        this.lockValue = UUID.randomUUID().toString()

        // 请求锁超时时间，纳秒
        val timeout = timeOut * 1000000

        // 系统当前时间，纳秒
        val nowTime = System.nanoTime()
        while (System.nanoTime() - nowTime < timeout) {
            if (setNxEx(lockKey, lockValue, expireTime.toLong())) {
                locked = true

                // 上锁成功结束请求
                return locked
            }

            // 每次请求等待一段时间
            sleep(10, 50000)
        }
        return locked
    }

    /**
     * 尝试获取锁 立即返回
     *
     * @return 是否成功获得锁
     */
    fun lock(): Boolean {
        lockValue = UUID.randomUUID().toString()
        //不存在则添加 且设置过期时间（单位ms）
        locked = setNxEx(lockKey, lockValue, expireTime.toLong())
        return locked
    }

    /**
     * 以阻塞方式的获取锁
     *
     * @return 是否成功获得锁
     */
    fun lockBlock(): Boolean {
        lockValue = UUID.randomUUID().toString()
        while (true) {
            //不存在则添加 且设置过期时间（单位ms）
            locked = setNxEx(lockKey, lockValue, expireTime.toLong())
            if (locked) {
                return locked
            }
            // 每次请求等待一段时间
            sleep(10, 50000)
        }
    }

    /**
     * 解锁
     * <p>
     * 可以通过以下修改，让这个锁实现更健壮：
     * <p>
     * 不使用固定的字符串作为键的值，而是设置一个不可猜测（non-guessable）的长随机字符串，作为口令串（token）。
     * 不使用 DEL 命令来释放锁，而是发送一个 Lua 脚本，这个脚本只在客户端传入的值和键的口令串相匹配时，才对键进行删除。
     * 这两个改动可以防止持有过期锁的客户端误删现有锁的情况出现。
     *
     * @return Boolean
     */
    fun unlock(): Boolean {
        // 只有加锁成功并且锁还有效才去释放锁
        return if (locked) {
            try {
                val keys: MutableList<String> = ArrayList()
                keys.add(lockKey)
                val args: MutableList<String> = ArrayList()
                args.add(lockValue)
                val result = client.eval(UNLOCK_LUA, keys, args) as Long
                locked = result == RELEASE_FAILED
                result == RELEASE_SUCCESS
            } catch (e: Throwable) {
                val value = this[lockKey, String::class.java]
                if (lockValue == value) {
                    client.delete(lockKey)
                    return true
                }
                false
            }
        } else true
    }

    /**
     * 重写redisTemplate的set方法
     * <p>
     * 命令 SET resource-name anystring NX EX max-lock-time 是一种在 Redis 中实现锁的简单方法。
     * <p>
     * 客户端执行以上的命令：
     * <p>
     * 如果服务器返回 OK ，那么这个客户端获得锁。
     * 如果服务器返回 NIL ，那么客户端获取锁失败，可以在稍后再重试。
     *
     * @param key     锁的Key
     * @param value   锁里面的值
     * @param seconds 过去时间（秒）
     * @return String
     */
    private fun setNxEx(key: String, value: String, seconds: Long): Boolean {
        val result: String = client.setNxEx(key, value, seconds)
        return LOCK_SUCCESS == result
    }

    /**
     * 获取redis里面的值
     *
     * @param key    key
     * @param aClass class
     * @return T
     */
    private operator fun <T> get(key: String, aClass: Class<T>): T? {
        return client.get(key, aClass)
    }

    /**
     * 线程等待时间
     *
     * @param millis 毫秒
     * @param nanos 纳秒
     */
    private fun sleep(millis: Long, nanos: Int) {
        Thread.sleep(millis, random.nextInt(nanos))
    }
}