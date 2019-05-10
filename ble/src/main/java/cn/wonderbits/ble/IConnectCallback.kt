package cn.wonderbits.ble

interface IConnectCallback {
    fun onFailed(msg: String)
    fun onConnected()

    fun onDisconnected()
}