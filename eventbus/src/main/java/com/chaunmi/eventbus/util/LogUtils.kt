package com.chaunmi.eventbus.util

object LogUtils {
    private const val TAG = "FlowEventBus"
    var DEBUG = true

    private var logger: ILogger? = DefaultLoggerImpl()

    fun setLogger(loggerImp: ILogger?) {
        logger = loggerImp
    }

    fun d(msg: String?, tr: Throwable? = null) {
        logger?.d(TAG, msg, tr)
    }

    fun e(msg: String?, tr: Throwable? = null) {
        logger?.e(TAG, msg, tr)
    }

    fun i(msg: String?, tr: Throwable? = null) {
        logger?.i(TAG, msg, tr)
    }

    fun w(msg: String?, tr: Throwable? = null) {
        logger?.w(TAG, msg, tr)
    }
}