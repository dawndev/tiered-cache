package com.github.dawndev.tieredcache.listener

interface IMessageService {

    /**
     * 拉消息
     *
     */
    fun pull()

    /**
     * 清空消息队列
     *
     */
    fun clearQueue()

    /**
     * 同步offset
     *
     */
    fun syncOffset()

    /**
     * 启动重连pub/sub检查
     *
     */
    fun reconnection()
}
