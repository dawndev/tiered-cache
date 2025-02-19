package com.github.dawndev.tieredcache.internal

import java.io.Serializable

object NullValue : Serializable {
    private fun readResolve(): Any = NullValue
    private const val serialVersionUID = 1L
}