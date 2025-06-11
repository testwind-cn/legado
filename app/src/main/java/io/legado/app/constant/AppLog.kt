package io.legado.app.constant

import android.util.Log
import io.legado.app.BuildConfig
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.LogUtils
import io.legado.app.utils.toastOnUi
import splitties.init.appCtx

/**
put
	在 recordLog  写文件
	在 DEBUG 或 recordLog 打印


putDebug
	在 recordLog  写文件
	在 DEBUG 且 recordLog 打印


RELEASE，AppConfig.recordLog=false
	put
			不写文件
			不打印
	putDebug
			不写文件
			不打印

RELEASE，AppConfig.recordLog=true
	put
			写文件
			打印
	putDebug
			写文件
			不打印

DEBUG，AppConfig.recordLog=false
	put
			不写文件
			打印
	putDebug
			不写文件
			不打印

DEBUG，AppConfig.recordLog=true
	put
			写文件
			打印
	putDebug
			写文件
			打印
 */
object AppLog {

    private val mLogs = arrayListOf<Triple<Long, String, Throwable?>>()

    val logs get() = mLogs.toList()

    @Synchronized
    fun put(message: String?, throwable: Throwable? = null, toast: Boolean = false, isDebugLog: Boolean = false) {
        message ?: return
        if (toast) {
            appCtx.toastOnUi(message)
        }
        if (mLogs.size > 100) {
            mLogs.removeLastOrNull()
        }
        if (throwable == null) {
            LogUtils.d("AppLog", message, isDebugLog)
        } else {
            LogUtils.d("AppLog", "$message\n${throwable.stackTraceToString()}", isDebugLog)
        }
        mLogs.add(0, Triple(System.currentTimeMillis(), message, throwable))
        // LogUtils.d 中的 androidLogHandler 复制打印 android.util.Log.d
        /*
        if (BuildConfig.DEBUG) {
            val stackTrace = Thread.currentThread().stackTrace
            Log.e(stackTrace[3].className, message, throwable)
        }
         */
    }

    @Synchronized
    fun putNotSave(message: String?, throwable: Throwable? = null, toast: Boolean = false) {
        message ?: return
        if (toast) {
            appCtx.toastOnUi(message)
        }
        if (mLogs.size > 100) {
            mLogs.removeLastOrNull()
        }
        mLogs.add(0, Triple(System.currentTimeMillis(), message, throwable))
        if (BuildConfig.DEBUG) {
            val stackTrace = Thread.currentThread().stackTrace
            Log.e(stackTrace[3].className, message, throwable)
        }
    }

    @Synchronized
    fun clear() {
        mLogs.clear()
    }

    fun putDebug(message: String?, throwable: Throwable? = null) {
        if (AppConfig.recordLog) {
            put(message, throwable, false, true)
        }
    }

}