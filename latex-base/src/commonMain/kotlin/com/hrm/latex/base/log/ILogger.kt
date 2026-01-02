package com.hrm.latex.base.log

/**
 * 日志接口，由外部在SDK初始化时注入实现
 */
interface ILogger {
    fun verbose(tag: String, message: String)
    fun debug(tag: String, message: String)
    fun info(tag: String, message: String)
    fun warn(tag: String, message: String)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}
