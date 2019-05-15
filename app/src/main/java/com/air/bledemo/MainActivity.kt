package com.air.bledemo

import android.content.Context
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
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*

private const val KEY_SP = "demo"
private const val KEY_COMMAND = "KEY_COMMAND"
private const val KEY_REQUEST = "KEY_REQUEST"

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
        WBBle.init(this)
            .setDebuggable(true) // 会输出日志和错误toast

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
        btn_command.setOnClickListener {
            var content = et_command.text.trim().toString()
            if (!contains(commandAdapter, content)) {
                commandAdapter.add(content)
            }
            content.replace("\"", "\'")
//            WBBle.get().writeCommand(content)
        }

        val requestAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayListOf())
        requestAdapter.add("ultrasonic1.get_distance()")
        et_request.setAdapter(requestAdapter)
        btn_request.setOnClickListener {
            val content = et_request.text.trim().toString()
            if (!contains(requestAdapter, content)) {
                requestAdapter.add(content)
            }
//            val content =
//                "display1.get_button_state()+display1.get_button_state()+display1.get_button_state()+display1.get_button_state()+display1.get_button_state()+display1.get_button_state()+display1.get_button_state()+display1.get_button_state()"
//            WBBle.get().writeRequest(content)
        }
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

    /* private fun saveCommndHistory(commadAdapter: ArrayAdapter<String>, newCommand: String) {
     }

     private fun saveRequestHistory(newRequest: String) {

     }

     private fun getHistory(key: String, defaultHistory: MutableSet<String>): MutableSet<String> {
         val sp = getSharedPreferences(KEY_SP, Context.MODE_PRIVATE)
         return sp.getStringSet(key, defaultHistory)
     }

     private fun getCommandHistory(): MutableSet<String> {
         return getHistory(KEY_COMMAND, mutableSetOf())
     }

     private fun getRequestHistory(): MutableSet<String> {
         return getHistory(KEY_REQUEST, mutableSetOf())
     }*/

    private fun setRl(visibility: Int) {
        rl_command.visibility = visibility
        rl_request.visibility = visibility
        tv_content.visibility = visibility
    }

    private var needScan = true
    override fun onResume() {
        super.onResume()
//        killServer()
        if (needScan) {
            startScan()
        }
    }

    val gson = Gson()
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
