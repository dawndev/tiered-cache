package com.github.dawndev.tieredcache.internal

import org.slf4j.Logger

/**
 * 将落地的值类型转换为用户值类型
 * @receiver Any?
 * @return Any?
 */
internal fun Any?.fromStoredValue(enableNull: Boolean): Any? {
    return if (enableNull && this@fromStoredValue is NullValue) {
        null
    } else this@fromStoredValue
}

/**
 * 将用户值转化为需要落地的值类型
 * @receiver Any? the given user value
 * @param enableNull Boolean
 * @return Any? the value to store
 */
internal fun Any?.toStoredValue(enableNull: Boolean): Any? {
    return if (enableNull && this@toStoredValue == null) {
        NullValue
    } else this@toStoredValue
}

internal fun Logger.taskIfDebug(lazyMessage: () -> String) {
    if (isDebugEnabled) {
        debug(lazyMessage())
    }
}

internal fun Logger.taskIfDebug(message: String, obj: Any?) {
    if (isDebugEnabled) {
        debug(message, obj)
    }
}

internal fun Logger.taskIfDebug(message: String, obj1: Any?, obj2: Any?) {
    if (isDebugEnabled) {
        debug(message, obj1, obj2)
    }
}

internal fun Logger.taskIfDebug(message: String, obj1: Any?, obj2: Any?, obj3: Any?) {
    if (isDebugEnabled) {
        debug(message, obj1, obj2, obj3)
    }
}
