package com.hrm.latex.sdk

import com.hrm.latex.base.log.HLog
import com.hrm.latex.base.log.ILogger

/**
 * LaTeX SDK 统一入口
 */
object LatexSDK {
    private var initialized = false
    
    /**
     * SDK 配置
     */
    data class Config(
        val logger: ILogger? = null,
        val debug: Boolean = false
    )
    
    /**
     * 初始化 SDK
     * @param config SDK 配置，可选传入日志实现
     */
    fun initialize(config: Config = Config()) {
        if (initialized) {
            HLog.w("LatexSDK", "SDK already initialized")
            return
        }
        
        // 注入日志实现
        HLog.setLogger(config.logger)
        
        initialized = true
        HLog.i("LatexSDK", "LaTeX SDK initialized, debug=${config.debug}")
    }
    
    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = initialized
}
