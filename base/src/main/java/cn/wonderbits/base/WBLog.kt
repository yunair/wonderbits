package cn.wonderbits.base

import android.util.Log

object WBLog {
    private var debug = true

    internal fun setDebuggable(debug: Boolean) {
        WBLog.debug = debug
    }

    fun d(msg: String) {
        if (debug) {
            Log.d("WBLog", msg)
        }
    }

    fun e(msg: String, t: Throwable? = null) {
        if (debug) {
            Log.e("WBLog", msg, t)
        }
    }

}