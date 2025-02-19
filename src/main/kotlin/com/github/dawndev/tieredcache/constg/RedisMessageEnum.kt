package com.github.dawndev.tieredcache.constg


enum class RedisMessageEnum(
    val desc: String
) {
    UNDEFINED("undefined"),

    EVICT("删除缓存"),

    CLEAR("清空缓存"),
    ;
}