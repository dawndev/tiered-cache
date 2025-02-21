package com.github.dawndev.tieredcache.config

/**
 * 多级缓存配置项
 *
 * @param localOptions                  一级缓存配置 [LocalCacheOptions]
 * @param remoteOptions                 二级缓存配置 [RemoteCacheOptions]
 * @param enableLocal                   是否使用一级缓存
 *
 * @author Espresso
 */
class MultiCacheOptions private constructor(
    val localOptions: LocalCacheOptions,
    val remoteOptions: RemoteCacheOptions,
    val enableLocal: Boolean = true,
)  {

    // 内部缓存名，由[一级缓存有效时间-二级缓存有效时间]组成
    var internalKey: String = ""

    val enableNull: Boolean
        get() = remoteOptions.enableNull

    init {
        internalKey()
    }

    private fun internalKey() {

        // 一级缓存有效时间-二级缓存有效时间
        val sb = StringBuilder()
        sb.append(localOptions.expiration)
        sb.append("-")
        sb.append(remoteOptions.expiration)
        internalKey = sb.toString()
    }

    class Builder {
        private lateinit var localOptions: LocalCacheOptions
        private lateinit var remoteOptions: RemoteCacheOptions
        private var enableLocal: Boolean = true

        fun localOptions(op: LocalCacheOptions) = apply { this.localOptions = op }
        fun remoteOptions(op: RemoteCacheOptions) = apply { this.remoteOptions = op }
        fun enableLocal(enableLocal: Boolean) = apply { this.enableLocal = enableLocal }

        fun build() = MultiCacheOptions(localOptions, remoteOptions, enableLocal)
    }

}