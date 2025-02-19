package com.github.dawndev.tieredcache.constg


enum class RedisMessageEnum(
    val desc: String
) {
    EVICT("删除缓存"),

    CLEAR("清空缓存"),
    ;
}