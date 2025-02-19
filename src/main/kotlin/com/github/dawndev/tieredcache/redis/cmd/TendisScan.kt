package com.github.dawndev.tieredcache.redis.cmd

import io.lettuce.core.dynamic.Commands
import io.lettuce.core.dynamic.annotation.Command

interface TendisScan : Commands {

    @Command("scan ?0 match ?1 count ?2 ?3")
    fun scan(cursor: Long, pattern: String, count: Int, nodeId: String): List<Any?>?

}