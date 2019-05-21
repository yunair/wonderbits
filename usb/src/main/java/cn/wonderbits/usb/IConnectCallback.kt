package cn.wonderbits.usb

interface IConnectCallback {
    fun onConnected()
    fun onFailed(msg: String)
}