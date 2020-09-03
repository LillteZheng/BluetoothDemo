package com.zhengsr.bluetoothdemo.bluetooth.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.zhengsr.bluetoothdemo.R

class BtServerActivity : AppCompatActivity() {
    companion object{
        private val TAG = javaClass.simpleName
    }

    /**
     * logic
     */
    private lateinit var bluetooth: BluetoothAdapter
    private lateinit var socketThread: AcceptThread
    private lateinit var handleSocket: HandleSocket

    /**
     * UI
     */
    private lateinit var serverStatusTv: TextView
    private lateinit var logTv: TextView
    private lateinit var sendMsgEd: EditText

    val stringBuffer = StringBuilder();
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bl_server)
        sendMsgEd = findViewById(R.id.send_edit)
        serverStatusTv = findViewById(R.id.servertv)
        logTv = findViewById(R.id.tv_log)

        bluetooth = BluetoothAdapter.getDefaultAdapter();
        socketThread = AcceptThread(readListener,writeListener)
        socketThread.start()


    }


    val readListener = object : HandleSocket.BluetoothListener {
        override fun onStart() {
            runOnUiThread{
                serverStatusTv.text = "服务器已就绪.."
            }
        }

        override fun onConnected(msg: String) {
            super.onConnected(msg)
            runOnUiThread{
                serverStatusTv.text = "已连接上客户端: $msg"
            }
        }




        override fun onReceiveData(socket: BluetoothSocket?,msg: String) {
            runOnUiThread {
                logTv.text = stringBuffer.run {
                    append(socket?.remoteDevice?.name+": "+msg).append("\n")
                    toString()
                }
            }
        }

        override fun onFail(error: String) {
            runOnUiThread{
                serverStatusTv.text = error
            }
        }

    }
    val writeListener = object : HandleSocket.BaseBluetoothListener {
        override fun onsendMsg(socket: BluetoothSocket?, msg: String) {
            runOnUiThread {
                logTv.text = stringBuffer.run {
                    append("我: $msg").append("\n")
                    toString()
                }
            }
        }


        override fun onFail(error: String) {
            Log.d(TAG, "zsr write onFail: $error")
        }

    }

    /**
     * 监听是否有设备接入
     */
    private inner class AcceptThread(val readListener: HandleSocket.BluetoothListener?,
                                     val writeListener: HandleSocket.BaseBluetoothListener?) : Thread() {


        private val serverSocket: BluetoothServerSocket? by lazy {
            //非明文匹配，不安全
            readListener?.onStart()
            bluetooth.listenUsingInsecureRfcommWithServiceRecord(TAG, BtBlueImpl.BLUE_UUID)
        }

        override fun run() {
            super.run()
            var shouldLoop = true
            while (shouldLoop) {
                var socket: BluetoothSocket? =
                    try {
                        //监听是否有接入
                        serverSocket?.accept()
                    } catch (e: Exception) {
                        Log.d(TAG, "zsr blue socket accept fail: ${e.message}")
                        shouldLoop = false
                        null
                    }
                socket?.also {
                    //拿到接入设备的名字
                    readListener?.onConnected(socket.remoteDevice.name)
                    //处理接收事件
                    handleSocket =
                        HandleSocket(socket)
                    handleSocket.start(readListener,writeListener)
                    //关闭服务端，只连接一个
                    serverSocket?.close()
                    shouldLoop = false;
                }
            }
        }

        fun cancel() {
            serverSocket?.close()
            handleSocket.cancel()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (!::socketThread.isInitialized) {
            socketThread.cancel()
        }

    }

    fun sendMsg(view: View) {
        if (::handleSocket.isInitialized) {
            handleSocket.sendMsg(sendMsgEd.text.toString())
            sendMsgEd.setText("")
        }
    }
}