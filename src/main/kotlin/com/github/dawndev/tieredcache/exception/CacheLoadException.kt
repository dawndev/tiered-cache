package com.github.dawndev.tieredcache.exception

import com.github.dawndev.tieredcache.internal.JsonUtils

class CacheLoadException(
    val key: Any,
    t: Throwable
) : RuntimeException(
    String.format("ERROR: Load cache | key:: %s ", JsonUtils.encodeToString(key)),
    t
)