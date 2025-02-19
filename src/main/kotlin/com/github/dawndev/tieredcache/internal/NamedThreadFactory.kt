package com.github.dawndev.tieredcache.internal

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

internal class NamedThreadFactory(
    private var name: String
) : ThreadFactory {

    private val poolNumber = AtomicInteger(1)

    private var threadGroup: ThreadGroup

    private val threadNumber = AtomicInteger(1)

    private var namePrefix: String

    init {
        val s = System.getSecurityManager()
        threadGroup = if (s != null) s.threadGroup else Thread.currentThread().threadGroup
        if (name.isBlank()) {
            name = "pool"
        }
        namePrefix = name + "-" + poolNumber.getAndIncrement() + "-thread-"
    }


    override fun newThread(runnable: Runnable): Thread {
        val thread = Thread(threadGroup, runnable, namePrefix + threadNumber.getAndIncrement(), 0)
        if (thread.isDaemon) {
            thread.isDaemon = false
        }
        if (thread.priority != Thread.NORM_PRIORITY) {
            thread.priority = Thread.NORM_PRIORITY
        }
        return thread
    }
}