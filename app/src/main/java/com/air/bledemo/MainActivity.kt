package com.air.bledemo

import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import cn.wonderbits.ble.BleScanDevice
import cn.wonderbits.ble.IConnectCallback
import cn.wonderbits.ble.IScanCallback
import cn.wonderbits.ble.WBBle
import cn.wonderbits.usb.WBUsb
import kotlinx.android.synthetic.main.activity_main.*


private const val KEY_SP = "demo"

fun Context.toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_FINE_LOCATION = 2
    }


    private val adapter = BleDeviceAdapter(arrayListOf())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setRl(View.GONE)
        // Initializes Bluetooth adapter.

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
        adapter.setClickAction {
            WBBle.get().connectDevice(this, it.address, object : IConnectCallback {
                override fun onConnected() {
                    runOnUiThread {
                        DeviceScanActivity.launch(this@MainActivity)
//                        rv.visibility = View.GONE
//                        setRl(View.VISIBLE)
                        toast("成功连接设备")
                    }
                }

                override fun onDisconnected() {
                }

                override fun onFailed(msg: String) {
                    runOnUiThread { toast(msg) }
                    Log.e(TAG, msg)
                }

            })
        }

        val commandAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayListOf())
        commandAdapter.add("display1.print(1, 1, 'hello world')")
        et_command.setAdapter(commandAdapter)
        var n = 0
        btn_command.setOnClickListener {
            //            WBUsb.get().writeCommand("display1.print(1, 1, 'hello world $n')")
            n++
            /* var content = et_command.text.trim().toString()
             if (!contains(commandAdapter, content)) {
                 commandAdapter.add(content)
             }
             content.replace("\"", "\'")*/
        }

        val requestAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayListOf())
        requestAdapter.add("ultrasonic1.get_distance()")
        et_request.setAdapter(requestAdapter)

        btn_request.setOnClickListener {
            //            WBUsb.get().writeRequest("ultrasonic1.get_distance()")
            val content = et_request.text.trim().toString()
            if (!contains(requestAdapter, content)) {
                requestAdapter.add(content)
            }
        }

        WBUsb.init(this)
        searchUsb()

        WBBle.init(this)
            .setDebuggable(true) // 会输出日志和错误toast

    }

    private fun searchUsb() {
        val availableDrivers = WBUsb.get().search()
        if (availableDrivers.isEmpty()) {
            return
        }
//        adapter.clear()
//        adapter.addAll(availableDrivers)
        // Open a connection to the first available driver.

        toast("正在连接")

        tv_content.postDelayed({
            WBUsb.get().connect(availableDrivers[0], object : cn.wonderbits.usb.IConnectCallback {
                override fun onConnected() {
                    DeviceScanActivity.launch(this@MainActivity)
                }

                override fun onFailed(msg: String) {
                }

            })
            rv.visibility = View.GONE
            setRl(View.VISIBLE)
//            toast("ports ${driver.ports.size}")
//            toast("ports ${driver.device.interfaceCount}")
//            serialManger.writeAsync("display1.print(1, 1, 'hello world')\r\n".toByteArray(Charsets.UTF_8))

        }, 3000)
    }


    /**
     * 获得授权USB的基本信息
     * 1、USB接口，一般是第一个
     * 2、USB设备的输入输出端
     *
     */
    private fun getUsbInfo(usbDevice: UsbDevice) {
        val sb = StringBuilder()
        when {
            Build.VERSION.SDK_INT >= 23 -> sb.append(
                String.format(
                    "VID:%04X  PID:%04X  ManuFN:%s  PN:%s V:%s",
                    usbDevice.vendorId,
                    usbDevice.productId,
                    usbDevice.manufacturerName,
                    usbDevice.productName,
                    usbDevice.version
                )
            )
            Build.VERSION.SDK_INT >= 21 -> sb.append(
                String.format(
                    "VID:%04X  PID:%04X  ManuFN:%s  PN:%s",
                    usbDevice.vendorId,
                    usbDevice.productId,
                    usbDevice.manufacturerName,
                    usbDevice.productName
                )
            )
            else -> sb.append(
                String.format(
                    "VID:%04X  PID:%04X",
                    usbDevice.vendorId,
                    usbDevice.productId
                )
            )
        }
        tv_content.visibility = View.VISIBLE
        tv_content.text = sb.toString()

//        connect()//连接
    }


    private fun saveHistory(key: String, value: MutableSet<String>) {
        val sp = getSharedPreferences(KEY_SP, Context.MODE_PRIVATE)
        sp.edit {
            putStringSet(key, value)
        }
    }

    private fun contains(adapter: ArrayAdapter<String>, content: String): Boolean {
        for (i in 0 until adapter.count) {
            if (adapter.getItem(i) == content) {
                return true
            }
        }
        return false
    }

    private fun setRl(visibility: Int) {
        rl_command.visibility = visibility
        rl_request.visibility = visibility
        tv_content.visibility = visibility
    }

    private var needScan = true
    override fun onResume() {
        super.onResume()
        if (needScan) {
//            startScan()
        }
    }

    private fun startScan() {
        WBBle.get().startScan(object : IScanCallback {
            override fun onFailed(msg: String) {
                WBBle.get().requestBluetoothEnable(this@MainActivity)
                Log.e(TAG, msg)
            }

            override fun onSuccess(devices: List<BleScanDevice>) {
                toast("扫描成功")
                needScan = false
                adapter.set(devices.map {
                    it.device
                })
            }

        })
    }
}
