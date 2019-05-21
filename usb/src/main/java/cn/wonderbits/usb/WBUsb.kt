package cn.wonderbits.usb

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import cn.wonderbits.base.WBBase
import cn.wonderbits.base.core.EventHandler
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.util.concurrent.Executors


private const val TAG = "WBUsb"

class WBUsb private constructor(private val context: Context) {
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private lateinit var serialManger: SerialInputOutputManager
    private val eventHandler: EventHandler
    private var port: UsbSerialPort? = null

    init {
        eventHandler = UsbEventHandler()
        WBBase.init(eventHandler)
    }

    fun search(): List<UsbSerialDriver> {
        return UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
    }

    fun connect(driver: UsbSerialDriver, connectCallback: IConnectCallback?) {
        port = driver.ports[0]
        serialManger = SerialInputOutputManager(port, serialIOManagerListener)
        val connection = usbManager.openDevice(driver.device)

        if (connection == null) {
            connectCallback?.onFailed("打开连接失败，可能没有权限")
            return
        }

        try {
            port?.apply {
                open(connection)
                setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            }

            try {
                port?.dtr = true
            } catch (x: IOException) {
                connectCallback?.onFailed("IOException DTR: " + x.message)
                return
            }

            try {
                port?.rts = true
            } catch (x: IOException) {
                connectCallback?.onFailed("IOException RTS: " + x.message)
                return
            }
        } catch (e: IOException) {
            Log.e("TAG", "Error setting up device: " + e.message)
            try {
                port?.close()
            } catch (e2: IOException) {
                Log.e(TAG, "port close")
            }
            connectCallback?.onFailed("Error setting up device: " + e.message)
            return
        }
        Thread {
            WBBase.get().start()
        }.start()
        connectCallback?.onConnected()
        Executors.newSingleThreadExecutor().submit(serialManger)
//        eventHandler.checkMsgList()
    }

    /* fun writeCommand(content: String) {
         eventHandler.writeCommand(content)
     }

     fun writeRequest(content: String) {
         eventHandler.writeRequest(content)
     }*/

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var usb: WBUsb

        @JvmStatic
        fun init(context: Context): Companion {
            usb = WBUsb(context.applicationContext)
            return this
        }

        @JvmStatic
        fun setDebuggable(debug: Boolean) {
            WBBase.setDebuggable(debug)
        }

        @JvmStatic
        fun get(): WBUsb {
            return usb
        }
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
            if (eventHandler.parseResponseFinished(messageString)) {
                // 解析完成，开始处理下一条指令
                eventHandler.checkMsgList()
            }
        }

        override fun onRunError(e: Exception?) {
            Log.e(TAG, "runner stop")
        }
    }

    private inner class UsbEventHandler : EventHandler(30) {
        override fun writeContent(content: String) {
            serialManger.writeAsync("$content\r\n".toByteArray(Charsets.UTF_8))
        }
    }

    fun close() {
        port?.close()
        WBBase.get().stop()
    }
}