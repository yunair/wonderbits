package com.air.bledemo

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import cn.wonderbits.ble.WonderBitsBle
import kotlinx.android.synthetic.main.activity_device_scan.*

class DeviceScanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_scan)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true

        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                Log.d("DeviceScanActivity", url)
                view.loadUrl(url)
                return true
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                Log.d("DeviceScanActivity", failingUrl)
                description?.let {
                    Log.d("DeviceScanActivity", it)
                }
                super.onReceivedError(view, errorCode, description, failingUrl)
            }


            @TargetApi(Build.VERSION_CODES.M)
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError) {
                Log.d("DeviceScanActivity", error.description.toString())
                Log.d("DeviceScanActivity", error.errorCode.toString())
                super.onReceivedError(view, request, error)
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler, error: SslError?) {
                handler.proceed()
                super.onReceivedSslError(view, handler, error)
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
                Log.d("DeviceScanActivity", message ?: "")
            }
        }
        web.loadUrl("file:///android_asset/index.html")
//        web.loadUrl("https://www.b4x.com:51041/")
    }

    companion object {
        fun launch(context: Context) {
            val intent = Intent(context, DeviceScanActivity::class.java)
            context.startActivity(intent)
        }
    }

    /*override fun onStop() {
        WonderBitsBle.get().close()
        super.onStop()
    }*/

    override fun onDestroy() {
        WonderBitsBle.get().close()
        super.onDestroy()
    }
}
