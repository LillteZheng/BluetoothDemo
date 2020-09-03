package com.zhengsr.bluetoothdemo.bluetooth.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemClickListener
import com.chad.library.adapter.base.listener.OnItemLongClickListener
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.zhengsr.bluetoothdemo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

class BtClientActivity : AppCompatActivity(), OnItemClickListener, OnItemLongClickListener {

    companion object{
        private val TAG =javaClass.simpleName
    }
    /**
     * UI
     */
    private var itemStateTv: TextView? = null
    private lateinit var logTv: TextView
    private lateinit var sendMsgEd:EditText

    /**
     * logic
     */
    private var blueBeans: MutableList<BluetoothDevice> = mutableListOf()
    private lateinit var blueAdapter: BlueAdapter
    private lateinit var bluetooth: BluetoothAdapter
    private val stringBuilder = StringBuilder()
    private var connectThread: ConnectThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bl_client)

        bluetooth = BluetoothAdapter.getDefaultAdapter()
        logTv = findViewById(R.id.tv_log)
        sendMsgEd = findViewById(R.id.send_edit)

        initRecyclerView()
        BtBlueImpl.init(this)
            .registerBroadcast()
            .foundDevices { dev ->

                if (dev !in blueBeans && dev.name != null) {
                    blueBeans.add(dev)
                    blueAdapter.notifyItemInserted(blueBeans.size)
                }
            }
    }


    /**
     * 初始化 recyclerview
     */
    private fun initRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler)
        val manager = LinearLayoutManager(this)

        recyclerView.layoutManager = manager
        blueAdapter =
            BlueAdapter(
                blueBeans,
                R.layout.recy_blue_item_layout
            )
        blueAdapter.animationEnable = true
        recyclerView.adapter = blueAdapter

        blueAdapter.setOnItemClickListener(this)
        blueAdapter.setOnItemLongClickListener(this)
    }


    /**
     * recyclerview 的 adapter
     */
    class BlueAdapter(datas: MutableList<BluetoothDevice>, layoutResId: Int) :
        BaseQuickAdapter<BluetoothDevice, BaseViewHolder>(layoutResId, datas) {
        override fun convert(holder: BaseViewHolder, item: BluetoothDevice) {

            holder.setText(R.id.blue_item_addr_tv, item.address)
            holder.setText(R.id.blue_item_name_tv, item.name)

            val statusTv = holder.getView<TextView>(R.id.blue_item_status_tv)
            if (item.bondState == BluetoothDevice.BOND_BONDED) {
                statusTv.text = "(已配对)"
                statusTv.setTextColor(Color.parseColor("#ff009688"))
            } else {
                statusTv.text = "(未配对)"
                statusTv.setTextColor(Color.parseColor("#ffFF5722"))

            }
        }

    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
        val dev: BluetoothDevice = blueBeans[position]
        Toast.makeText(this, "开始连接...", Toast.LENGTH_SHORT).show()
        itemStateTv = view.findViewById(R.id.blue_item_status_tv)
        connectThread = ConnectThread(dev, readListener, writeListener)
        connectThread?.start()
    }

    override fun onItemLongClick(
        adapter: BaseQuickAdapter<*, *>,
        view: View,
        position: Int
    ): Boolean {
       //todo 解绑估计需要系统 api，后面看看源码是怎么实现的

        return true
    }




    /**
     * 扫描蓝牙
     */
    fun scan(view: View) {
        blueBeans.clear()
        blueAdapter.notifyDataSetChanged()
        BtBlueImpl.foundDevices { bean ->
                if (bean !in blueBeans && bean.name != null) {
                    blueBeans.add(bean)
                    blueAdapter.notifyItemInserted(blueBeans.size)
                }
            }
    }


    val job = Job()
    val coroutineScope = CoroutineScope(job)

    /**
     * 连接类
     */

    inner class ConnectThread(
        val device: BluetoothDevice, val readListener: HandleSocket.BluetoothListener?,
        val writeListener: HandleSocket.BaseBluetoothListener?
    ) : Thread() {
        var handleSocket: HandleSocket? = null
        private val socket: BluetoothSocket? by lazy {
            readListener?.onStart()
            //监听该 uuid
            device.createRfcommSocketToServiceRecord(BtBlueImpl.BLUE_UUID)
        }

        override fun run() {
            super.run()
            //下取消
            bluetooth.cancelDiscovery()
            try {

                socket.run {
                    //阻塞等待
                    this?.connect()
                    //连接成功，拿到服务端设备名
                    socket?.remoteDevice?.let { readListener?.onConnected(it.name) }

                    //处理 socket 读写
                    handleSocket =
                        HandleSocket(this)
                    handleSocket?.start(readListener, writeListener)

                }
            } catch (e: Exception) {
                readListener?.onFail(e.message.toString())
            }
        }

        fun cancel() {
            socket?.close()
            handleSocket?.cancel()
        }
    }


    fun sendMsg(view: View) {
        connectThread?.handleSocket?.sendMsg(sendMsgEd.text.toString())
        sendMsgEd.setText("")

    }

    override fun onDestroy() {
        super.onDestroy()
        connectThread?.cancel()
        BtBlueImpl.release()
    }

    val readListener = object : HandleSocket.BluetoothListener {
        override fun onStart() {
            runOnUiThread {
                itemStateTv?.text = "正在连接..."
            }
        }

        override fun onReceiveData(socket: BluetoothSocket?,msg: String) {
            runOnUiThread {
                logTv.text = stringBuilder.run {
                    append(socket?.remoteDevice?.name+": "+msg).append("\n")
                    toString()
                }
            }
        }

        override fun onConnected(msg: String) {
            super.onConnected(msg)
            runOnUiThread {
                itemStateTv?.text = "已连接"
            }
        }

        override fun onFail(error: String) {
            runOnUiThread {
                logTv.text = stringBuilder.run {
                    append(error).append("\n")
                    toString()
                }
                itemStateTv?.text = "已配对"
            }
        }

    }
    val writeListener = object : HandleSocket.BaseBluetoothListener {

        override fun onsendMsg(socket: BluetoothSocket?, msg: String) {
            runOnUiThread {
                logTv.text = stringBuilder.run {
                    append("我: $msg").append("\n")
                    toString()
                }
            }
        }
        override fun onFail(error: String) {
           // Log.d(com.zhengsr.bluetoothdemo.TAG, "zsr write onFail: $error")
            logTv.text = stringBuilder.run {
                append("发送失败: $error").append("\n")
                toString()
            }
        }

    }


}