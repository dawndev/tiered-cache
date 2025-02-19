package com.github.dawndev.tieredcache.exception


class SerializationException : NestedRuntimeException {

    /**
     * Constructs a new `SerializationException` instance.
     *
     * @param msg   msg
     * @param cause 原因
     */
    constructor(msg: String, cause: Throwable) : super(msg, cause)

    /**
     * Constructs a new `SerializationException` instance.
     *
     * @param msg msg
     */
    constructor(msg: String) : super(msg)
}