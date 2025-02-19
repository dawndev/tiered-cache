package com.github.dawndev.tieredcache.internal

object CollectionUtils {

    fun isEmpty(collection: Collection<*>?): Boolean {
        return collection == null || collection.isEmpty()
    }

    fun isEmpty(map: Map<*, *>?): Boolean {
        return map == null || map.isEmpty()
    }
}