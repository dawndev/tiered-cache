package com.github.dawndev.tieredcache.internal


import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.LockSupport

/**
 * 等待线程容器
 *
 * @author jdg
 */
class AwaitThreadContainer {

    private val threadMap: ConcurrentHashMap<String, MutableSet<Thread>> = ConcurrentHashMap()

    /**
     * 线程等待,最大等待100毫秒
     * @param key 缓存Key
     * @param milliseconds 等待时间
     * @throws InterruptedException [InterruptedException]
     */
    @Throws(InterruptedException::class)
    fun await(key: String, milliseconds: Int) {

        // 测试当前线程是否已经被中断
        if (Thread.interrupted()) {
            throw InterruptedException()
        }
        var threadSet = threadMap[key]

        // 判断线程容器是否是null，如果是就新创建一个
        if (threadSet == null) {
            threadSet = ConcurrentSkipListSet(
                Comparator.comparing(Thread::toString)
            )
            threadMap[key] = threadSet
        }

        // 将线程放到容器
        threadSet.add(Thread.currentThread())

        // 阻塞一定的时间
        LockSupport.parkNanos(this, TimeUnit.MILLISECONDS.toNanos(milliseconds.toLong()))
    }

    /**
     * 线程唤醒
     *
     * @param key
     */
    fun signalAll(key: String) {
        val threadSet = threadMap[key]
        if (threadSet == null || threadSet.isEmpty()) {
            return
        }
        synchronized(threadSet) {
            if (threadSet.isNotEmpty()) {
                for (thread in threadSet) {
                    LockSupport.unpark(thread)
                }
                // 清空等待线程容器
                threadSet.clear()
            }
        }
    }
}
