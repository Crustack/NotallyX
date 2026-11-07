package com.philkes.notallyx.core.platform

/**
 * Platform-independent logging contract replacing direct `android.util.Log` calls in shared domain,
 * data, and presentation code.
 */
interface Logger {
    fun d(tag: String, message: String)

    fun i(tag: String, message: String)

    fun w(tag: String, message: String, throwable: Throwable? = null)

    fun e(tag: String, message: String, throwable: Throwable? = null)
}

object AppLogger : Logger {
    @Volatile
    var delegate: Logger =
        object : Logger {
            override fun d(tag: String, message: String) {
                println("D/$tag: $message")
            }

            override fun i(tag: String, message: String) {
                println("I/$tag: $message")
            }

            override fun w(tag: String, message: String, throwable: Throwable?) {
                println("W/$tag: $message")
                throwable?.printStackTrace()
            }

            override fun e(tag: String, message: String, throwable: Throwable?) {
                System.err.println("E/$tag: $message")
                throwable?.printStackTrace()
            }
        }

    override fun d(tag: String, message: String) = delegate.d(tag, message)

    override fun i(tag: String, message: String) = delegate.i(tag, message)

    override fun w(tag: String, message: String, throwable: Throwable?) =
        delegate.w(tag, message, throwable)

    override fun e(tag: String, message: String, throwable: Throwable?) =
        delegate.e(tag, message, throwable)
}
