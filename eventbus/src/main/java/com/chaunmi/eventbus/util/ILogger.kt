package com.chaunmi.eventbus.util

import java.util.logging.Level

interface ILogger {
    fun e(tag: String?, msg: String?, tr: Throwable?)
    fun d(tag: String?, msg: String?, tr: Throwable?)
    fun i(tag: String?, msg: String?, tr: Throwable?)
    fun w(tag: String?, msg: String?, tr: Throwable?)
}