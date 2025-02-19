package com.github.dawndev.tieredcache.constg

enum class CacheModeEnum(
    val desc: String
) {
    ONLY_FIRST( "只是用一级缓存"),
    ONLY_SECOND("只是使用二级缓存"),
    ALL(        "同时开启一级缓存和二级缓存");
}