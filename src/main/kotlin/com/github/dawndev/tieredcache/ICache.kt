package com.github.dawndev.tieredcache

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

import java.util.concurrent.Callable

/**
 * Cache
 *
 * @author Espresso
 */
interface ICache {

    /**
     * 缓存名称
     */
    val name: String

    /**
     * 返回Cache对象
     *
     * @return
     */
    fun getNativeCache(): Any

    /**
     * 根据KEY返回缓存中对应的值，并将其返回类型转换成对应类型，如果对应key不存在返回NULL
     *
     * @param key        缓存key
     * @param resultType 返回值类型
     * @param <T>        Object
     * @return 缓存key对应的值
     */
    fun <T> get(key: String, resultType: Class<T>): T?

    /**
     * 根据KEY返回缓存中对应的值，并将其返回类型转换成对应类型，如果对应key不存在则调用valueLoader加载数据
     *
     * @param key         缓存key
     * @param resultType  返回值类型
     * @param valueLoader 加载缓存的回调方法
     * @param <T>         Object
     * @return 缓存key对应的值
     */
    fun <T> get(key: String, resultType: Class<T>, valueLoader: Callable<T>): T?

    /**
     * 将对应key-value放到缓存，如果key原来有值就直接覆盖
     *
     * @param key   缓存key
     * @param value 缓存的值
     */
    fun put(key: String, value: Any?)

    /**
     * 如果缓存key没有对应的值就将值put到缓存，如果有就直接返回原有的值
     * <p>就相当于:
     * <pre><code>
     * Object existingValue = cache.get(key);
     * if (existingValue == null) {
     *     cache.put(key, value);
     *     return null;
     * } else {
     *     return existingValue;
     * }
     * </code></pre>
     * except that the action is performed atomically. While all out-of-the-box
     * {@link CacheManager} implementations are able to perform the put atomically,
     * the operation may also be implemented in two steps, e.g. with a check for
     * presence and a subsequent put, in a non-atomic way. Check the documentation
     * of the native cache implementation that you are using for more details.
     *
     * @param key        缓存key
     * @param value      缓存key对应的值
     * @param resultType 返回值类型
     * @param <T> T
     * @return 因为值本身可能为NULL，或者缓存key本来就没有对应值的时候也为NULL，
     * 所以如果返回NULL就表示已经将key-value键值对放到了缓存中
     * @since 4.1
     */
    fun <T> putIfAbsent(key: String, value: Any?, resultType: Class<T>): T?

    /**
     * 在缓存中删除对应的key
     *
     * @param key 缓存key
     */
    fun evict(key: String)

    /**
     * 清除缓存
     */
    fun clear()

    /**
     * 缓存预估size
     *
     * @return 预估大小
     */
    fun estimatedSize(): Long {
        return 0
    }
}