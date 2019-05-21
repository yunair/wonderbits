package cn.wonderbits.base

import cn.wonderbits.base.core.EventHandler
import cn.wonderbits.base.core.WBSocket

class WBBase private constructor(private val eventHandler: EventHandler) {
    companion object {
        lateinit var impl: WBBase
        @JvmStatic
        fun init(eventHandler: EventHandler) {
            impl = WBBase(eventHandler)
            WBSocket.setEventHandler(eventHandler)
        }

        @JvmStatic
        fun setDebuggable(debug: Boolean) {
            WBLog.setDebuggable(debug)
            WBUtils.setToastable(debug)
        }

        @JvmStatic
        fun get(): WBBase {
            return impl
        }
    }

    fun start() {
        eventHandler.checkMsgList()
        WBSocket.start()
    }

    fun stop() {
        WBSocket.stop()
    }
}