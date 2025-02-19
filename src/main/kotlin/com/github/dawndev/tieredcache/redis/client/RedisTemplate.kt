package com.github.dawndev.tieredcache.redis.client

import com.github.dawndev.tieredcache.listener.RedisMessageListener
import com.github.dawndev.tieredcache.redis.serializer.RedisSerializer
import java.util.concurrent.TimeUnit


/**
 * Redis client
 *
 * @author jdg
 */
interface RedisTemplate {

    /**
     * key序列化方式
     *
     */
    var keySerializer: RedisSerializer

    /**
     * value序列化方式
     */
    var valueSerializer: RedisSerializer

    /**
     * 通过key获取储存在redis中的value,自动转对象
     *
     * @param key        key
     * @param resultType 返回值类型对应的Class对象
     * @param <T>        返回值类型
     * @return 成功返回value 失败返回null
     */
    fun <T> get(key: String, resultType: Class<T>): T?

    /**
     * 通过key获取储存在redis中的value,自动转对象
     *
     * @param key                  key
     * @param resultType           返回值类型对应的Class对象
     * @param valueRedisSerializer 指定序列化器
     * @param <T>                  返回值类型
     * @return 成功返回value 失败返回null
     */
    fun <T> get(key: String, resultType: Class<T>, valueRedisSerializer: RedisSerializer): T?

    /**
     * <p>
     * 向redis存入key和value,并释放连接资源
     * </p>
     * <p>
     * 如果key已经存在 则覆盖
     * </p>
     *
     * @param key   key
     * @param value value
     * @return 成功 返回OK 失败返回 0
     */
    fun set(key: String, value: Any)

    /**
     * <p>
     * 向redis存入key和value,并释放连接资源
     * </p>
     * <p>
     * 如果key已经存在 则覆盖
     * </p>
     *
     * @param key   key
     * @param value value
     * @param time  时间
     * @param unit  时间单位
     * @return 成功 返回OK 失败返回 0
     */
    fun set(key: String, value: Any, time: Long, unit: TimeUnit)

    /**
     * <p>
     * 向redis存入key和value,并释放连接资源
     * </p>
     * <p>
     * 如果key已经存在 则覆盖
     * </p>
     *
     * @param key                  key
     * @param value                value
     * @param time                 时间
     * @param unit                 时间单位
     * @param valueRedisSerializer 指定序列化器
     * @return 成功 返回OK 失败返回 0
     */
    fun set(key: String, value: Any, time: Long, unit: TimeUnit, valueRedisSerializer: RedisSerializer)

    /**
     * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1
     * GB).
     *
     * @param key   key
     * @param value value
     * @param time  expire time in the units of <code>expx</code>
     * @return Status code reply
     */
    fun setNxEx(key: String, value: Any, time: Long): String

    /**
     * <p>
     * 删除指定的key,也可以传入一个包含key的数组
     * </p>
     *
     * @param keys 一个key 也可以使 string 数组
     * @return 返回删除成功的个数
     */
    fun delete(vararg keys: String): Long

    /**
     * <p>
     * 删除一批key
     * </p>
     *
     * @param keys key的Set集合
     * @return 返回删除成功的个数
     */
    fun delete(keys: Set<String>): Long

    /**
     * <p>
     * 判断key是否存在
     * </p>
     *
     * @param key key
     * @return true OR false
     */
    fun hasKey(key: String): Boolean

    /**
     * <p>
     * 为给定 key 设置生存时间，当 key 过期时(生存时间为 0 )，它会被自动删除。
     * </p>
     *
     * @param key      key
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 成功返回1 如果存在 和 发生异常 返回 0
     */
    fun expire(key: String, timeout: Long, timeUnit: TimeUnit)

    /**
     * <p>
     * 以秒为单位，返回给定 key 的剩余生存时间
     * </p>
     *
     * @param key key
     * @return 当 key 不存在时或没有设置剩余生存时间时，返回 -1 。否则，以秒为单位，返回 key
     * 的剩余生存时间。 发生异常 返回 0
     */
    fun getExpire(key: String): Long

    /**
     * <p>
     * 查询符合条件的key
     * </p>
     *
     * @param pattern 表达式
     * @return 返回符合条件的key
     */
    fun scan(pattern: String): Set<String>

    /**
     * <p>
     * 通过key向list头部添加字符串
     * </p>
     *
     * @param key                  key
     * @param valueRedisSerializer 指定序列化器
     * @param values               可以使一个string 也可以使string数组
     * @return 返回list的value个数
     */
    fun lpush(key: String, valueRedisSerializer: RedisSerializer, vararg values: String): Long

    /**
     * <p>
     * 通过key返回list的长度
     * </p>
     *
     * @param key key
     * @return long
     */
    fun llen(key: String): Long

    /**
     * <p>
     * 通过key获取list指定下标位置的value
     * </p>
     * <p>
     * 如果start 为 0 end 为 -1 则返回全部的list中的value
     * </p>
     *
     * @param key                  key
     * @param start                起始位置
     * @param end                  结束位置
     * @param valueRedisSerializer 指定序列化器
     * @return List
     */
    fun lrange(key: String, start: Long, end: Long, valueRedisSerializer: RedisSerializer): List<String?>

    /**
     * 执行Lua脚本
     *
     * @param script Lua 脚本
     * @param keys   参数
     * @param args   参数值
     * @return 返回结果
     */
    fun eval(script: String, keys: List<String>, args: List<String>): Any

    /**
     * 发送消息
     *
     * @param channel 发送消息的频道
     * @param message 消息内容
     * @return Long
     */
    fun publish(channel: String, message: String): Long

    /**
     * 绑定监听器
     *
     * @param messageListener 消息监听器
     * @param channel         信道
     */
    fun subscribe(messageListener: RedisMessageListener, vararg channel: String)
}