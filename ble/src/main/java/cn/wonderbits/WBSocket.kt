package cn.wonderbits

import cn.wonderbits.ble.WonderBitsBle
import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.ExceptionListener
import io.netty.channel.ChannelHandlerContext


internal object WBSocket {
    private val server by lazy {
        createServer()
    }

    private fun createServer(): SocketIOServer {
        val config = Configuration()
        config.hostname = "localhost"
        config.port = 8082
        config.socketConfig.isReuseAddress = true
        config.exceptionListener = object : ExceptionListener {
            override fun onConnectException(e: java.lang.Exception?, client: SocketIOClient?) {
            }

            override fun onEventException(e: java.lang.Exception?, args: MutableList<Any>?, client: SocketIOClient?) {
            }

            override fun onDisconnectException(e: java.lang.Exception?, client: SocketIOClient?) {
            }

            override fun exceptionCaught(ctx: ChannelHandlerContext?, e: Throwable?): Boolean {
                ctx?.close()
                return false
            }

        }

        val server = SocketIOServer(config)

        server.addEventListener<String>("mfe-message", String::class.java) { client, data, ackRequest ->
            WonderBitsBle.get().writeCommand(data)
            //            server.broadcastOperations.sendEvent("mfe-message", data)
        }

        server.addEventListener<String>("mfe-reporter", String::class.java) { client, data, ackRequest ->
            WonderBitsBle.get().writeRequest(data)
            //            server.broadcastOperations.sendEvent("mfe-message", data)
        }

        server.addConnectListener {
            WBLog.d("conntect success")
            server.broadcastOperations.sendEvent("connect", "socket已连接")
        }
        server.addDisconnectListener {
            WBLog.d("disconntect success")

            server.broadcastOperations.sendEvent("disconnect", "socket已断开")
        }
        return server
    }

    fun sendEvent(key: String, value: String) {
        server.broadcastOperations.sendEvent(key, value)
    }

    fun sendMfeData(data: String) {
        sendEvent("mfe-data", data)
    }


    fun start() {
//        stop()
        server.start()
//        val future = server.startAsync()
//        future.addListener { future -> future?.isSuccess?.let { action(it) } }
    }

    fun stop() {
        try {
            server.stop()
        } catch (e: Exception) {
            WBLog.e("", e)
        } finally {
//            server = createServer()
        }
    }
}