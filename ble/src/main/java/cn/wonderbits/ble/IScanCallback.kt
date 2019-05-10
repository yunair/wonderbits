package cn.wonderbits.ble

interface IScanCallback {
    fun onFailed(msg: String)
    fun onSuccess(devices: List<BleScanDevice>)
}