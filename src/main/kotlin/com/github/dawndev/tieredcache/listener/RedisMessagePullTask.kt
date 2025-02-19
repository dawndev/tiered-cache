package com.github.dawndev.tieredcache.listener

import com.github.dawndev.tieredcache.internal.NamedThreadFactory
import com.github.dawndev.tieredcache.manage.AbstractCacheManager
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * redis消息拉模式
 *
 * @author jdg
 */
class RedisMessagePullTask(
    cacheManager: AbstractCacheManager
) {
    /**
     * redis消息处理器
     */
    private val redisMessageService: IMessageService = RedisMessageService(cacheManager)

    init {

        // 1. 服务启动同步最新的偏移量
        redisMessageService.syncOffset()

        // 2. 启动PULL TASK
        startPullTask()

        // 3. 启动重置本地偏消息移量任务
        clearMessageQueueTask()

        // 4. 重连检测
        reconnectionTask()
    }

    /**
     * 启动PULL TASK
     */
    private fun startPullTask() {
        executor.scheduleWithFixedDelay({
            try {
                redisMessageService.pull()
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("gwan-cache PULL 方式清楚一级缓存异常：{}", e.message, e)
            }
        }, 5, 30, TimeUnit.SECONDS)
    }

    /**
     * 启动清空消息队列的任务
     */
    private fun clearMessageQueueTask() {
        val cal = Calendar.getInstance()
        cal[Calendar.HOUR_OF_DAY] = 3
        cal[Calendar.MINUTE] = 0
        cal[Calendar.SECOND] = 0
        var initialDelay = System.currentTimeMillis() - cal.timeInMillis
        initialDelay = if (initialDelay > 0) initialDelay else 0

        // 每天晚上凌晨3:00执行任务
        executor.scheduleWithFixedDelay({
            try {
                redisMessageService.clearQueue()
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("gwan-cache 重置本地消息偏移量异常：{}", e.message, e)
            }
        }, initialDelay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS)
    }

    /**
     * 启动重连pub/sub检查
     */
    private fun reconnectionTask() {
        executor.scheduleWithFixedDelay({ redisMessageService.reconnection() },
            5, 5, TimeUnit.SECONDS)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RedisMessagePullTask::class.java)

        val executor: ScheduledThreadPoolExecutor =
            ScheduledThreadPoolExecutor(3, NamedThreadFactory("tiered-cache-pull-message"))
    }
}
