package com.philkes.notallyx.core.platform

class DesktopLogger : Logger {
    override fun d(tag: String, message: String) {
        println("[DEBUG] [$tag] $message")
    }

    override fun i(tag: String, message: String) {
        println("[INFO] [$tag] $message")
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        System.err.println("[WARN] [$tag] $message")
        throwable?.printStackTrace(System.err)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        System.err.println("[ERROR] [$tag] $message")
        throwable?.printStackTrace(System.err)
    }
}
