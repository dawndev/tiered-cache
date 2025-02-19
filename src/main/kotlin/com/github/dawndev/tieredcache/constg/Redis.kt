package com.github.dawndev.tieredcache.constg

const val BLANK = ""

const val REDIS_DEFAULT_PORT = 6379

const val LOCAL_HOST = "localhost"

const val NORMAL_LIFECYCLE = ONE_DAY_SEC

// 常缓存有效时间一个月 /秒
const val NORMAL_MONTH_LIFECYCLE = ONE_DAY_SEC * 30

// 锁的有效时间
const val LOCK_LIFECYCLE = ONE_MINUTE_MILLS.toInt()

// 没有拿到锁的等待时间
const val LOCK_WAIT_TIME = ONE_SECOND_MILLS

// 调用set后的返回值
const val LOCK_SUCCESS = "OK"

// 当key不存在时
const val SET_IF_NOT_EXIST = "NX"

// 毫秒
const val SET_WITH_EXPIRE_TIME_MILLS = "PX"

// 秒
const val SET_WITH_EXPIRE_TIME_SEC = "EX"

const val RELEASE_FAILED = 0L

const val RELEASE_SUCCESS = 1L

// 默认Redis连接超时时间为：1000毫秒
const val REDIS_CONNECT_TIMEOUT = 3600

// 默认Redis数据库为：0
const val DEFAULT_DB = 0

// 默认Redis连接池大小为：128
const val DEFAULT_POOL_SIZE = 128

// 默认Redis连接池最大空闲连接数为：64
const val DEFAULT_MAX_IDLE = 64

// 默认Redis连接池获取连接最大等待时间为：3000毫秒
const val DEFAULT_MAX_WAIT = 3000L

const val UNLOCK_LUA: String = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"