package cn.wonderbits

import android.content.Context
import android.widget.Toast

internal object WBUtils {
    private var debug = true
    fun setToastable(debug: Boolean) {
        this.debug = debug
    }

    fun toast(context: Context, text: String) {
        if (debug) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}