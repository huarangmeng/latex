package com.hrm.latex.base.log

/**
 * 日志门面，通过 LatexSDK 初始化时注入实现
 */
object HLog {
    private var loggerImpl: ILogger? = null

    /**
     * 设置日志实现，由 SDK 初始化时调用
     */
    fun setLogger(logger: ILogger?) {
        loggerImpl = logger
    }

    fun v(tag: String, message: String) {
        loggerImpl?.verbose(tag, message)
    }

    fun d(tag: String, message: String) {
        loggerImpl?.debug(tag, message)
    }

    fun i(tag: String, message: String) {
        loggerImpl?.info(tag, message)
    }

    fun w(tag: String, message: String) {
        loggerImpl?.warn(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        loggerImpl?.error(tag, message, throwable)
    }
}
