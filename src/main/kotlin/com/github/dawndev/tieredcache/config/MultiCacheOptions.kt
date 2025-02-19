package com.github.dawndev.tieredcache.config

/**
 * 多级缓存配置项
 *
 * @param l1Options                  一级缓存配置 [LocalCacheOptions]
 * @param l2Options                  二级缓存配置 [RemoteCacheOptions]
 * @param enableL1                   是否使用一级缓存
 *
 * @author Espresso
 */
data class MultiCacheOptions(
    val l1Options: LocalCacheOptions,
    val l2Options: RemoteCacheOptions,
    val enableL1: Boolean = true,
)  {

    // 内部缓存名，由[一级缓存有效时间-二级缓存有效时间]组成
    var internalKey: String = ""

    init {
        internalKey()
    }

    private fun internalKey() {

        // 一级缓存有效时间-二级缓存有效时间
        val sb = StringBuilder()
        sb.append(l1Options.timeUnit.toMillis(l1Options.expireTime.toLong()))
        sb.append(SPLIT)
        sb.append(l2Options.timeUnit.toMillis(l2Options.expiration))
        internalKey = sb.toString()
    }

    companion object {
        val SPLIT: String = "-"
    }
}
