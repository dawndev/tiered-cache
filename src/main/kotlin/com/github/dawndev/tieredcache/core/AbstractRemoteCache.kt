package com.github.dawndev.tieredcache.core

import com.github.dawndev.tieredcache.core.remote.RemoteKey
import com.github.dawndev.tieredcache.internal.NullValue
import com.github.dawndev.tieredcache.internal.taskIfDebug
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

abstract class AbstractRemoteCache<REMOTE_KEY: RemoteKey>(
    override val name: String,
    override val enableNull: Boolean,
    private val preloadTime: Long,
    private val enableForceRefresh: Boolean,
    private val magnification: Int
) : AbstractCache(name, enableNull) {

    private val logger = LoggerFactory.getLogger(AbstractRemoteCache::class.java)

    /**
     * 刷新缓存数据
     */
    protected open fun <T> refreshCache(remoteKey: REMOTE_KEY, resultType: Class<T>, valueLoader: Callable<T>, result: Any?) {
        var preload = preloadTime
        // 允许缓存NULL值，则自动刷新时间也要除以倍数
        val flag = enableNull && (result is NullValue || result == null)
        if (flag) {
            preload = preload / magnification
        }
        if (this.isRefresh(remoteKey, preload)) {
            // 判断是否需要强制刷新在开启刷新线程
            if (!enableForceRefresh) {
                logger.taskIfDebug("二级缓存 key={} 软刷新缓存模式", remoteKey.getKey())
                this.softRefresh(remoteKey)
            } else {
                logger.taskIfDebug("二级缓存 key={} 强刷新缓存模式", remoteKey.getKey())
                this.forceRefresh(remoteKey, resultType, valueLoader, preload)
            }
        }
    }

    /**
     * 软刷新，直接修改缓存时间
     * @param remoteKey REMOTE_KEY
     */
    protected abstract fun softRefresh(remoteKey: REMOTE_KEY)

    /**
     * 硬刷新（执行被缓存的方法）
     * @param redisCacheKey REMOTE_KEY
     * @param resultType Class<T>
     * @param valueLoader Callable<T>
     * @param preloadTime Long
     */
    protected abstract fun <T> forceRefresh(
        redisCacheKey: REMOTE_KEY,
        resultType: Class<T>,
        valueLoader: Callable<T>,
        preloadTime: Long
    )

    protected abstract fun isRefresh(remoteKey: REMOTE_KEY, preloadTime: Long): Boolean
}