//MIT License
//
//Copyright (c) 2025 Espresso
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in all
//copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//SOFTWARE.

package com.github.dawndev.tieredcache

import com.github.dawndev.tieredcache.config.MultiCacheOptions
import com.github.dawndev.tieredcache.core.ICache

/**
 * 缓存管理器
 * 允许通过缓存名称来获的对应的 [ICache].
 *
 * @author Espresso
 */
interface CacheManager {

    /**
     * 根据缓存名称返回对应的[ICache]
     *
     * @param name String
     * @return ICache?
     */
    fun getCache(name: String): ICache?

    /**
     * 获取所有缓存名称的集合
     *
     * @return 所有缓存名称的集合
     */
    fun getCacheNames(): Collection<String>

    /**
     * 根据缓存名称返回对应的[ICache]，如果没有找到就新建一个并放到容器
     *
     * @param name                  缓存名称
     * @param multiCacheOptions     多级缓存配置
     * @return [ICache]
     */
    fun registerCache(name: String, multiCacheOptions: MultiCacheOptions): ICache?

    /**
     * 取消注册[ICache]
     * @param name String
     */
    fun unregisterCache(name: String)

    @Throws(Exception::class)
    fun destroy()
}