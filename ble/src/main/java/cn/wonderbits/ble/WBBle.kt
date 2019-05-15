package cn.wonderbits.ble

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Handler
import cn.wonderbits.WBLog
import cn.wonderbits.WBSocket
import cn.wonderbits.WBUtils
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.*

private const val SCAN_PERIOD: Long = 1000
private const val REQUEST_ENABLE_BT = 1
private const val MSG_CHECK_MSG_LIST = 2

private const val MARK_COMMAND = "#_command"
private const val MARK_REQUEST = "#_request"

private const val KEY_TERMINAL = ">>> "

private val SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
private val WRITE_CHARACTERISTIC_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
private val READ_CHARACTERISTIC_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")


class WBBle private constructor(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager =
            context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val BluetoothAdapter.isDisabled: Boolean
        get() = !isEnabled

    private val handler = Handler(Handler.Callback {
        if (it.what == MSG_CHECK_MSG_LIST) {
            if (msgList.isNotEmpty() && needProcessNext()) {
                val msg = msgList.removeAt(0)
                // 每次都取后一位, 来判断走到一个命令结束与否
                if (msgList.isNotEmpty()) {
                    when (msgList[0]) {
                        MARK_COMMAND -> {
                            msgList.removeAt(0)
                            setCommand()
                        }
                        MARK_REQUEST -> {
                            msgList.removeAt(0)
                            setRequest()
                        }
                        else -> {
                        }
                    }
                }

                writeContent(msg)

            } else {
                checkMsgList()
            }
        }

        return@Callback false
    })

    private fun checkMsgList() {

//        handler.sendEmptyMessageAtTime(MSG_CHECK_MSG_LIST, SystemClock.uptimeMillis() + 110)
        handler.sendEmptyMessageDelayed(MSG_CHECK_MSG_LIST, 100)
    }

    private var scanning: Boolean = false
    private var connected = false
    private var initialized = false
    private var waitFinishedKey = ""

    private fun processNext() {
        waitFinishedKey = ""
    }

    private fun processCurrent() {
        waitFinishedKey = keyList.removeAt(0)
    }

    private fun needProcessNext(): Boolean {
        return waitFinishedKey == ""
    }


    companion object {
        private var ble: WBBle? = null
        @JvmStatic
        fun init(context: Context): Companion {
            ble = WBBle(context.applicationContext)
            return this
        }

        @JvmStatic
        fun setDebuggable(debug: Boolean) {
            WBLog.setDebuggable(debug)
            WBUtils.setToastable(debug)
        }

        @JvmStatic
        fun get(): WBBle {
            val ins = ble
            if (ins == null) {
                throw IllegalStateException("还未初始化")
            } else {
                return ins
            }
        }
    }

    private fun hasPermission(): Boolean {
        val bleAdapter = bluetoothAdapter
        if (bleAdapter == null || bleAdapter.isDisabled) {
            return false
        }

        return true
    }

    fun requestBluetoothEnable(activity: Activity) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    private var connectCallback: IConnectCallback? = null
    private var receivedMsg = ""
    fun connectDevice(content: Context, address: String, callback: IConnectCallback) {
//        WBSocket.stop()
        this.connectCallback = callback
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
            gatt = device.connectGatt(content, true, gattClientCallback)
        } else {
            WBUtils.toast(context, "蓝牙地址不合法")
            WBLog.e("蓝牙地址不合法 $address")
        }
    }

    private var bleScanCallback: IScanCallback? = null

    fun startScan(callback: IScanCallback) {
        this.bleScanCallback = callback
        if (!hasPermission()) {
            bleScanCallback?.onFailed("蓝牙未打开")
            return
        }

        disconnectGattServer()
        val filters = arrayListOf<ScanFilter>()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        scanner?.let {
            it.startScan(filters, settings, scanCallback)
            scanning = true
            handler.postDelayed({
                stopScan()
            }, SCAN_PERIOD)
        }
    }

    private val gattClientCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (status == BluetoothGatt.GATT_FAILURE) {
                connectCallback?.onFailed("连接蓝牙设备失败，错误原因未知")
                disconnectGattServer()
                return
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                connectCallback?.onFailed("连接蓝牙设备失败，错误代码$status")
                disconnectGattServer()
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected = true
                gatt?.discoverServices()
                WBSocket.start()
                checkMsgList()
                connectCallback?.onConnected()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer()
                connectCallback?.onDisconnected()
            }
        }

        // used to signify that our Characteristic is fully ready for use

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                WBLog.d("Device service discovery unsuccessful, status $status")
                return
            }


            val service = gatt.getService(SERVICE_UUID)
            val characteristic = service.getCharacteristic(WRITE_CHARACTERISTIC_UUID)
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
//            initialized = gatt.setCharacteristicNotification(characteristic, true)
            val readCharacteristic = service.getCharacteristic(READ_CHARACTERISTIC_UUID)
            initialized = gatt.setCharacteristicNotification(readCharacteristic, true)
            if (initialized) {
                WBLog.d("Characteristic notification set successfully for " + characteristic.uuid.toString())
            } else {
                WBLog.d("Characteristic notification set failure for " + characteristic.uuid.toString())
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            readCharacteristic(characteristic)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
//            WBLog.d("onCharacteristicWrite")
            checkMsgList()
        }

        /* override fun onCharacteristicRead(
             gatt: BluetoothGatt?,
             characteristic: BluetoothGattCharacteristic?,
             status: Int
         ) {
             super.onCharacteristicRead(gatt, characteristic, status)
             WBLog.d("onCharacteristicRead")
         }

         override fun onDescriptorRead(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
             super.onDescriptorRead(gatt, descriptor, status)
             WBLog.d("onDescriptorRead")
         }

         override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
             super.onDescriptorWrite(gatt, descriptor, status)
             WBLog.d("onDescriptorWrite")
         }*/
    }

    private fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        val bytes = characteristic.value
        var log = ""
        for (byte in bytes) {
            val byteStr = String.format("%02x", byte)
            log += byteStr
        }
//        WBLog.d("log: $log")
        var messageString: String? = null
        try {
            messageString = String(bytes, Charset.forName("UTF-8"))
//            messageString = messageString.trim()
        } catch (e: UnsupportedEncodingException) {
            WBLog.d("Unable to convert message bytes to string")
        }

        receivedMsg += messageString
        if (receivedMsg.endsWith(KEY_TERMINAL) && messageString == KEY_TERMINAL) {
            receivedMsg = receivedMsg.removeSuffix(KEY_TERMINAL)
            if (isCommand) {
                // 命令结束，发送key

                WBSocket.sendEvent(waitFinishedKey, "")
                WBLog.d("Received message $waitFinishedKey isCommand: $receivedMsg")
            } else if (isRequest) {
                val msgs = receivedMsg.split("\r\n")
                WBLog.d("Received message $waitFinishedKey isRequest: ${msgs[0]}")
//                WBLog.d("Received message isRequest: ${msgs[1]}")
                WBSocket.sendEvent(waitFinishedKey, msgs[1])
                // 消息结束，打印此返回值
                WBLog.d("Received message isRequest: $receivedMsg")
            }

            receiveNextMsg()
            return
        }
//        WBLog.d("Received message isRequest: $messageString")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            bleScanCallback?.onFailed("蓝牙扫描出错，状态为 $errorCode")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                addScanResult(it)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach {
                addScanResult(it)
            }
        }
    }

    private val scanResults = hashMapOf<String, BleScanDevice>()
    private fun addScanResult(result: ScanResult) {
        val device = result.device
        val deviceAddress = device.address
        scanResults[deviceAddress] = BleScanDevice(result.rssi, device)
    }

    private fun stopScan() {
        val bleAdapter = bluetoothAdapter
        val scanner = bleAdapter?.bluetoothLeScanner
        if (scanning && bleAdapter != null && bleAdapter.isEnabled && scanner != null) {
            scanner.stopScan(scanCallback)
            scanComplete()
        }

        scanning = false
    }

    private fun scanComplete() {
        if (scanResults.isEmpty()) {
            return
        }
        val list = arrayListOf<BleScanDevice>()
        for (device in scanResults.entries) {
            WBLog.d("Found device: ${device.key}")
            list.add(device.value)
        }
        list.sortBy { it.rssi }
        this.bleScanCallback?.onSuccess(list)
    }

    private fun splitContent(content: String): ArrayList<String> {
        val contentList = arrayListOf<String>()
        if (content.length > 15) {
            var subContent = content
            while (subContent.length > 15) {
                val needContent = subContent.substring(0, 15)
                subContent = subContent.removeRange(0, 15)
                contentList.add(needContent)
            }
            contentList.add(subContent + "\r\n")
        } else {
            contentList.add(content + "\r\n")
        }
        return contentList
    }

    private var index = -1

    private fun getCurrentDataIndex(): String {
        val cIndex = index + 1
        return when {
            cIndex in 0..9 -> "0$cIndex"
            cIndex >= 100 -> {
                index = -1
                "00"
            }
            else -> "$cIndex"
        }

    }

    private var gatt: BluetoothGatt? = null
    private var isCommand = false
    private var isRequest = false

    // 处理到这个时候需要超时等待

    private fun setCommand() {
        isRequest = false
        isCommand = true
        processCurrent()
    }

    private fun setRequest() {
        isRequest = true
        isCommand = false
        processCurrent()
    }

    private fun resetKey(content: String) {
        msgList.add(0, content)
        if (!isRequest && !isCommand) {
            return
        }
        keyList.add(0, waitFinishedKey)
        if (isRequest) {
            isRequest = false
            msgList.add(1, MARK_REQUEST)
        } else if (isCommand) {
            isCommand = false
            msgList.add(1, MARK_COMMAND)
        }

        waitFinishedKey = ""
    }


    internal fun writeCommand(content: String) {
        keyList.add(content)
        val contents = splitContent(content)
        for (i in contents.indices) {
            msgList.add(contents[i])
        }
        msgList.add(MARK_COMMAND)
    }

    //    private val valueCallbacks = arrayListOf<IValueCallback>()
    internal fun writeRequest(content: String) {
//        valueCallbacks.add(callback)
        keyList.add(content)
        val contents = splitContent(content)
        for (i in contents.indices) {
            msgList.add(contents[i])
        }
        msgList.add(MARK_REQUEST)
    }

    private var msgList = arrayListOf<String>()
    private var keyList = arrayListOf<String>()

    private fun writeContent(content: String) {
        if (!initialized) {
            return
        }
        gatt?.let {
            val service = it.getService(SERVICE_UUID)
            val characteristic = service.getCharacteristic(WRITE_CHARACTERISTIC_UUID)
            if (characteristic == null) {
                WBUtils.toast(context, "无法写入数据")
                WBLog.d("can not write value")
                return@let
            }
            val value = "${getCurrentDataIndex()}$content"
            WBLog.d("write value $value")
            characteristic.value = value.toByteArray(Charset.forName("UTF-8"))
            val result = it.writeCharacteristic(characteristic)
            WBLog.d("write result: $result")
            if (!result) {
//                WBUtils.toast(context, "写入数据失败")
//                WBSocket.sendEvent(waitFinishedKey, "wonderbits_failed")
//                receiveNextMsg()
                resetKey(content)
            } else {
                index++
            }
        }
    }

    private fun receiveNextMsg() {
        receivedMsg = ""
        processNext()
    }

    private fun disconnectGattServer() {
        connected = false
        gatt?.apply {
            disconnect()
            close()
        }
    }

    fun close() {
        connected = false
        gatt?.apply {
            disconnect()
            close()
        }
        WBSocket.stop()

    }
}