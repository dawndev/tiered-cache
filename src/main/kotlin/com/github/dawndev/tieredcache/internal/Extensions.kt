package com.github.dawndev.tieredcache.internal

import org.slf4j.Logger

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