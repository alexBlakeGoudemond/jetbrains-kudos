package com.kudos.infrastructure

import com.intellij.openapi.diagnostic.Logger

/**
 * Convenience wrapper. Helpful to enforce a consistent prefix for all log messages related to Window Accent.
 */
class KudosLogger(private val logger: Logger) {

    private val prefix = "[Kudos]"

    fun info(message: String, t: Throwable? = null) {
        if (t != null) {
            logger.info("$prefix $message", t)
        } else {
            logger.info("$prefix $message")
        }
    }

    fun warn(message: String, t: Throwable? = null) {
        if (t != null) {
            logger.warn("$prefix $message", t)
        } else {
            logger.warn("$prefix $message")
        }
    }

    fun error(message: String, t: Throwable? = null) {
        if (t != null) {
            logger.error("$prefix $message", t)
        } else {
            logger.error("$prefix $message")
        }
    }

    fun debug(message: String, t: Throwable? = null) {
        if (t != null) {
            logger.debug("$prefix $message", t)
        } else {
            logger.debug("$prefix $message")
        }
    }

}

inline fun <reified T> kudosLogger(): KudosLogger {
    return KudosLogger(Logger.getInstance(T::class.java))
}