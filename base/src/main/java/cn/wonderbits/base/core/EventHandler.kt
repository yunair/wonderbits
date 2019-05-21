package cn.wonderbits.base.core

import android.os.Handler
import cn.wonderbits.base.WBLog

private const val MSG_CHECK_MSG_LIST = 2

private const val MARK_COMMAND = "#_command"
private const val MARK_REQUEST = "#_request"

private const val KEY_TERMINAL = ">>> "

abstract class EventHandler(private val checkMsgIntervalMills: Long) {
    private val handler = Handler(Handler.Callback {
        if (it.what == MSG_CHECK_MSG_LIST) {
//            WBLog.d("check msg1: ${msgList.isNotEmpty()}")
//            WBLog.d("check msg: ${needProcessNext()}")
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

    private fun receiveNextMsg() {
        receivedMsg = ""
        processNext()
//        checkMsgList()
    }

    fun checkMsgList() {
//        WBLog.d("looper: ${handler.looper == Looper.getMainLooper()}")
        handler.sendEmptyMessageDelayed(MSG_CHECK_MSG_LIST, checkMsgIntervalMills)
    }

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

    protected fun resetKey(content: String) {
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
        checkMsgList()
    }

    protected var msgList = arrayListOf<String>()
    private var keyList = arrayListOf<String>()

    fun writeCommand(content: String) {
        keyList.add(content)
        addToMsgList(content)
        msgList.add(MARK_COMMAND)
    }

    //    private val valueCallbacks = arrayListOf<IValueCallback>()
    fun writeRequest(content: String) {
        keyList.add(content)
        addToMsgList(content)
        msgList.add(MARK_REQUEST)
    }

    open fun addToMsgList(content: String) {
        msgList.add(content)
    }


    // 真正将数据写到对应的通道
    abstract fun writeContent(content: String)

    @Volatile
    private var receivedMsg = ""

    fun parseResponseFinished(messageString: String): Boolean {
        receivedMsg += messageString
        if (receivedMsg.endsWith(KEY_TERMINAL) && messageString.endsWith(KEY_TERMINAL)) {
            receivedMsg = receivedMsg.removeSuffix(KEY_TERMINAL)
            var finished = false
            if (isCommand) {
                // 命令结束，发送key

                WBSocket.sendEvent(waitFinishedKey, "")
                WBLog.d("Received message $waitFinishedKey isCommand: $receivedMsg")
                finished = true
            } else if (isRequest) {
                val msgs = receivedMsg.split("\r\n")
                WBLog.d("Received message $waitFinishedKey isRequest: ${msgs[0]}")
//                WBLog.d("Received message isRequest: ${msgs[1]}")
                WBSocket.sendEvent(waitFinishedKey, msgs[1])
                // 消息结束，打印此返回值
                WBLog.d("Received message isRequest: $receivedMsg")
                finished = true
            }

            receiveNextMsg()
            return finished
        }
        return false
    }
}