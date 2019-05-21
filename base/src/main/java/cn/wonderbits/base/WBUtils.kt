package cn.wonderbits.base

import android.content.Context
import android.widget.Toast

object WBUtils {
    private var debug = true
    internal fun setToastable(debug: Boolean) {
        WBUtils.debug = debug
    }

    fun toast(context: Context, text: String) {
        if (debug) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}