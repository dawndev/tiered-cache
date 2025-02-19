package com.github.dawndev.tieredcache.exception


abstract class NestedRuntimeException : RuntimeException {

    /**
     * Construct a `NestedRuntimeException` with the specified detail message.
     * @param msg the detail message
     */
    constructor(msg: String) : super(msg) {}

    /**
     * Construct a `NestedRuntimeException` with the specified detail message
     * and nested exception.
     * @param msg the detail message
     * @param cause the nested exception
     */
    constructor(msg: String, cause: Throwable) : super(msg, cause) {}

    /**
     * Return the detail message, including the message from the nested exception
     * if there is one.
     */
    override val message: String?
        get() = buildMessage(super.message, cause)

    /**
     * Retrieve the innermost cause of this exception, if any.
     * @return the innermost exception, or `null` if none
     * @since 2.0
     */
    val rootCause: Throwable?
        get() = getRootCause(this)

    /**
     * Retrieve the most specific cause of this exception, that is,
     * either the innermost cause (root cause) or this exception itself.
     *
     * Differs from [.getRootCause] in that it falls back
     * to the present exception if there is no root cause.
     * @return the most specific cause (never `null`)
     * @since 2.0.3
     */
    val mostSpecificCause: Throwable
        get() {
            val rootCause: Throwable? = rootCause
            return rootCause ?: this
        }

    /**
     * Check whether this exception contains an exception of the given type:
     * either it is of the given class itself or it contains a nested cause
     * of the given type.
     * @param exType the exception type to look for
     * @return whether there is a nested exception of the specified type
     */
    operator fun contains(exType: Class<*>?): Boolean {
        if (exType == null) {
            return false
        }
        if (exType.isInstance(this)) {
            return true
        }
        var cause = cause
        if (cause === this) {
            return false
        }
        return if (cause is NestedRuntimeException) {
            cause.contains(exType)
        } else {
            while (cause != null) {
                if (exType.isInstance(cause)) {
                    return true
                }
                if (cause.cause === cause) {
                    break
                }
                cause = cause.cause
            }
            false
        }
    }

    companion object {
        /** Use serialVersionUID from Spring 1.2 for interoperability  */
        private const val serialVersionUID = 5439915454935047936L

        init {
            // Eagerly load the NestedExceptionUtils class to avoid classloader deadlock
            // issues on OSGi when calling getMessage(). Reported by Don Brown; SPR-5607.
            this::class.java.name
        }


        fun buildMessage(message: String?, cause: Throwable?): String? {
            if (cause == null) {
                return message
            }
            val sb = StringBuilder(64)
            if (message != null) {
                sb.append(message).append("; ")
            }
            sb.append("nested exception is ").append(cause)
            return sb.toString()
        }

        fun getRootCause(original: Throwable?): Throwable? {
            if (original == null) {
                return null
            }
            var rootCause: Throwable? = null
            var cause = original.cause
            while (cause != null && cause !== rootCause) {
                rootCause = cause
                cause = cause.cause
            }
            return rootCause
        }

        fun getMostSpecificCause(original: Throwable?): Throwable? {
            val rootCause = getRootCause(original)
            return rootCause ?: original
        }
    }
}
