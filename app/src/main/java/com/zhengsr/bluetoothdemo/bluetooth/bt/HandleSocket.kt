package com.zhengsr.bluetoothdemo.bluetooth.bt

import android.bluetooth.BluetoothSocket
import com.zhengsr.bluetoothdemo.utils.close
import kotlinx.coroutines.*
import java.io.DataInputStream
import java.io.OutputStream

/**
 * @author by zhengshaorui 2020/7/29 16:46
 * describe：BluetoothSocket 处理读写时间
 */
class HandleSocket(private val socket: BluetoothSocket?) {
    private lateinit var readThread: ReadThread
    private lateinit var writeThread: WriteThread

    companion object {
        private val TAG = HandleSocket::class.java.simpleName
    }


    fun start(
        readlisterner: BluetoothListener?,
        writelistener: BaseBluetoothListener?
    ) {
        readThread = ReadThread(
            socket,
            readlisterner
        )
        readThread.start()
        
        writeThread = WriteThread(socket, writelistener)
    }


    /**
     * 读取数据
     */
    private class ReadThread(
        val socket: BluetoothSocket?,
        val bluetoothListener: BaseBluetoothListener?
    ) : Thread() {

        //拿到 BluetoothSocket 的输入流
        private val inputStream: DataInputStream? = DataInputStream(socket?.inputStream)
        private var isDone = false
        private val listener: BluetoothListener? =
            bluetoothListener as BluetoothListener

        //todo 目前简单数据，暂时使用这种
        private val byteBuffer: ByteArray = ByteArray(1024)
        override fun run() {
            super.run()
            var size: Int? = null
            while (!isDone) {
                try {
                    //拿到读的数据和大小
                    size = inputStream?.read(byteBuffer)
                } catch (e: Exception) {
                    isDone = false
                    e.message?.let { listener?.onFail(it) }
                    return
                }


                if (size != null && size > 0) {
                    //把结果公布出去
                    listener?.onReceiveData(socket,String(byteBuffer, 0, size))
                } else {
                    //如果接收不到数据，则证明已经断开了
                    listener?.onFail("断开连接")
                    isDone = false;
                }
            }
        }

        fun cancel() {
            isDone = false;
            socket?.close()
            close(inputStream)
        }
    }

    /**
     * 写数据
     */
    private val job = Job()
    private val scope = CoroutineScope(job)

    inner class WriteThread(
        private val socket: BluetoothSocket?,
        val listener: BaseBluetoothListener?
    ) {

        private var isDone = false

        //拿到 socket 的 outputstream
        private val dataOutput: OutputStream? = socket?.outputStream

        fun sendMsg(msg: String) {
            if (isDone) {
                return
            }
            scope.launch(Dispatchers.Main) {
                val result = withContext(Dispatchers.IO) {
                    sendScope(msg)
                }

                if (result != null) {
                    listener?.onFail(result)
                }else{
                    listener?.onsendMsg(socket,msg)
                }

            }
        }

        //实际发送的类
        private fun sendScope(msg: String): String? {
            return try {
                //写数据
                dataOutput?.write(msg.toByteArray())
                dataOutput?.flush()
                null
            } catch (e: Exception) {
                e.toString()
            }
        }

        fun cancel() {
            isDone = true
            socket?.close()
            close(dataOutput)
        }


    }

    fun sendMsg(string: String) {
        writeThread.sendMsg(string)
    }

    interface BaseBluetoothListener {
        fun onsendMsg(socket: BluetoothSocket?,msg: String){}
        fun onFail(error: String)
    }


    interface BluetoothListener :
        BaseBluetoothListener {
        fun onStart()
        fun onReceiveData(socket: BluetoothSocket?,msg: String)
        fun onConnected(msg: String) {}

    }

    /**
     * 关闭连接
     */
    fun cancel() {
        readThread?.cancel()
        writeThread?.cancel()
        close(socket)
        job.cancel()
    }

}