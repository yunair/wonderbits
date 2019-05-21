package cn.wonderbits

import cn.wonderbits.base.WBSocket
import cn.wonderbits.base.core.EventHandler

class WBBase(private val eventHandler: EventHandler) {
    fun start() {
        WBSocket.start()
    }

    fun stop() {
        WBSocket.stop()
    }
}