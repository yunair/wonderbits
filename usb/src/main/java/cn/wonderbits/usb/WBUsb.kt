package cn.wonderbits.usb

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Handler
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.concurrent.Executors


private const val MSG_CHECK_MSG_LIST = 1

private const val MARK_COMMAND = "#_command"
private const val MARK_REQUEST = "#_request"

private const val KEY_TERMINAL = ">>> "
private const val TAG = "WBUsb"

class WBUsb private constructor(val context: Context) {
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private lateinit var serialManger: SerialInputOutputManager
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
        handler.sendEmptyMessageDelayed(MSG_CHECK_MSG_LIST, 30)
    }

    fun search(): List<UsbSerialDriver> {
        return UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
    }

    fun connect(driver: UsbSerialDriver, action: (Boolean) -> Unit) {
        val port = driver.ports[0]
        serialManger = SerialInputOutputManager(port, serialIOManagerListener)
        val connection = usbManager.openDevice(driver.device)

        if (connection == null) {
            action(false)
            return
        }

        try {
            port.open(connection)
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

            try {
                port.dtr = true
            } catch (x: IOException) {
                action(false)
                Log.e(TAG, "IOException DTR: " + x.message)
                return
            }

            try {
                port.rts = true
            } catch (x: IOException) {
                action(false)
                Log.e(TAG, "IOException RTS: " + x.message)
                return
            }

            try {
                port.purgeHwBuffers(true, true)
            } catch (e: IOException) {
                Log.e(TAG, "IOException purgeHwBuffers: " + e.message)
                return
            }
        } catch (e: IOException) {
            Log.e("TAG", "Error setting up device: " + e.message)
            try {
                port.close()
            } catch (e2: IOException) {
                Log.e(TAG, "port close")
            }
            action(false)
            return
        }
        Thread {
            WBSocket.start()
        }.start()
        action(true)
        Executors.newSingleThreadExecutor().submit(serialManger)
        checkMsgList()
    }


    private var keyList = arrayListOf<String>()

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

    private var isCommand = false
    private var isRequest = false
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

    private val msgList = arrayListOf<String>()

    internal fun writeCommand(content: String) {
        keyList.add(content)
        msgList.add(content)
        msgList.add(MARK_COMMAND)
    }

    //    private val valueCallbacks = arrayListOf<IValueCallback>()
    internal fun writeRequest(content: String) {
        keyList.add(content)
        msgList.add(content)
        msgList.add(MARK_REQUEST)
    }

    private fun writeContent(content: String) {
        serialManger.writeAsync("$content\r\n".toByteArray(Charsets.UTF_8))
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var usb: WBUsb? = null

        @JvmStatic
        fun init(context: Context): WBUsb.Companion {
            usb = WBUsb(context.applicationContext)
            return this
        }

        @JvmStatic
        fun get(): WBUsb {
            val ins = usb
            if (ins == null) {
                throw IllegalStateException("还未初始化")
            } else {
                return ins
            }
        }
    }

    @Volatile
    var receivedMsg = ""

    private fun receiveNextMsg() {
        receivedMsg = ""
        processNext()
        checkMsgList()
    }

    private val serialIOManagerListener = object : SerialInputOutputManager.Listener {
        override fun onNewData(data: ByteArray) {
            var messageString = ""
            try {
                messageString = String(data, Charset.forName("UTF-8"))
            } catch (e: UnsupportedEncodingException) {
                Log.d(TAG, "Unable to convert message bytes to string")
//                WBLog.d("Unable to convert message bytes to string")
            }
            Log.d(TAG, messageString)
            receivedMsg += messageString
            if (receivedMsg.endsWith(KEY_TERMINAL) && messageString.endsWith(KEY_TERMINAL)) {
                receivedMsg = receivedMsg.removeSuffix(KEY_TERMINAL)
                Log.d(TAG, "receivedMsg : $receivedMsg")
                if (isCommand) {
                    // 命令结束，发送key
                    WBSocket.sendEvent(waitFinishedKey, "")
//                    WBLog.d("Received message $waitFinishedKey isCommand: $receivedMsg")
                } else if (isRequest) {
                    val msgs = receivedMsg.split("\r\n")
//                    WBLog.d("Received message $waitFinishedKey isRequest: ${msgs[0]}")
//                WBLog.d("Received message isRequest: ${msgs[1]}")
                    WBSocket.sendEvent(waitFinishedKey, msgs[1])
                    // 消息结束，打印此返回值
//                    WBLog.d("Received message isRequest: $receivedMsg")
                }
                receiveNextMsg()
            }
        }

        override fun onRunError(e: Exception?) {
            Log.e(TAG, "runner stop")
        }

    }
}