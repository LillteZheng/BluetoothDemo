package com.zhengsr.bluetoothdemo.bluetooth.a2dp

import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.zhengsr.bluetoothdemo.R
import com.zhengsr.bluetoothdemo.bluetooth.bt.BlueBroadcastListener
import com.zhengsr.bluetoothdemo.bluetooth.bt.BtBlueImpl
import com.zhengsr.bluetoothdemo.utils.close

class A2dpActivity : AppCompatActivity() {

    companion object {
        private val TAG = javaClass.simpleName
    }

    /**
     * UI
     */
    private var itemStateTv: TextView? = null
    private var logTv: TextView? = null

    /**
     * logic
     */
    private var blueBeans: MutableList<BluetoothDevice> = mutableListOf()
    private lateinit var blueAdapter: BlueAdapter
    private lateinit var bluetooth: BluetoothAdapter
    private var bluetoothA2dp: BluetoothA2dp? = null
    private var connectThread: ConnectThread? = null
    private val stringBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a2dp)

        logTv = findViewById(R.id.log_tv)

        bluetooth = BluetoothAdapter.getDefaultAdapter()
        initRecyclerView()

        val broadcast = listOf<String>(
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED
        )


        BtBlueImpl.init(this)
            .registerBroadcast(broadcast, blueStateListener)
            .foundDevices { bean ->
                if (bean !in blueBeans && bean.name != null) {
                    blueBeans.add(bean)
                    blueAdapter.notifyItemInserted(blueBeans.size)
                }
            }


        bluetooth.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
            override fun onServiceDisconnected(profile: Int) {

                if (profile == BluetoothProfile.A2DP) {
                    bluetoothA2dp = null
                }
            }

            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                if (profile == BluetoothProfile.A2DP) {
                    bluetoothA2dp = proxy as BluetoothA2dp


                }
            }

        }, BluetoothProfile.A2DP)
    }

    /**
     * 初始化 recyclerview
     */
    private fun initRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler)
        val manager = LinearLayoutManager(this)

        recyclerView.layoutManager = manager
        blueAdapter = BlueAdapter(
            blueBeans,
            R.layout.recy_blue_item_layout
        )
        blueAdapter.animationEnable = true
        recyclerView.adapter = blueAdapter

        blueAdapter.setOnItemClickListener { baseQuickAdapter: BaseQuickAdapter<*, *>, view: View, i: Int ->
            val dev: BluetoothDevice = blueBeans[i]
            Toast.makeText(this, "开始连接...", Toast.LENGTH_SHORT).show()
            itemStateTv = view.findViewById(R.id.blue_item_status_tv)

            connectThread = ConnectThread(dev, object :
                ConnectListener {
                override fun onStart() {
                    Log.d(TAG, "zsr onStart: ")
                }

                override fun onConnected() {
                    Log.d(TAG, "zsr onConnected: ")
                }

                override fun onFail(errorMsg: String) {
                    Log.d(TAG, "zsr onFail: $errorMsg")
                }

            })
            connectThread?.start()

        }
    }

    inner class ConnectThread(
        private val device: BluetoothDevice,
        private val listener: ConnectListener
    ) : Thread() {
        private var socket: BluetoothSocket? = null

        override fun run() {
            super.run()
            listener.onStart()
            bluetooth.cancelDiscovery()
            while (true) {
                try {
                    //先绑定
                    if (device.bondState != BluetoothDevice.BOND_BONDED) {
                        val createSocket =
                            BluetoothDevice::class.java.getMethod(
                                "createRfcommSocket",
                                Int::class.java
                            )
                        createSocket.isAccessible = true
                        //找一个通道去连接即可，channel 1～30
                        socket = createSocket.invoke(device, 1) as BluetoothSocket
                        //阻塞等待
                        socket?.connect()
                        //延时，以便于去连接
                        sleep(2000)
                    }

                    if (connectA2dp(device)) {
                        listener.onConnected()
                        break
                    } else {
                        listener.onFail("Blue connect fail ")
                    }

                } catch (e: Exception) {
                    listener.onFail(e.toString())
                    return
                }
            }
        }

        fun cancel() {
            close(socket)
            //取消绑定
            try {
                //通过反射获取BluetoothA2dp中connect方法（hide的），断开连接。
                val connectMethod = BluetoothA2dp::class.java.getMethod(
                    "disconnect",
                    BluetoothDevice::class.java
                )
                connectMethod.invoke(bluetoothA2dp, device)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun connectA2dp(device: BluetoothDevice):Boolean{
        //连接 a2dp
        val connect =
            BluetoothA2dp::class.java.getMethod("connect", BluetoothDevice::class.java)
        connect.isAccessible = true
        return connect.invoke(bluetoothA2dp, device) as Boolean
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


    val blueStateListener = object :
        BlueBroadcastListener {
        override fun invoke(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val dev = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val  state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,0)
                    Log.d(TAG, "zsr invoke: $state && ${dev.name}")
                    //connectA2dp(dev)
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothA2dp.EXTRA_STATE,
                        BluetoothA2dp.STATE_DISCONNECTED
                    )
                    if (state == BluetoothA2dp.STATE_CONNECTING) {

                        itemStateTv?.text = "正在连接..."
                    } else if (state == BluetoothA2dp.STATE_CONNECTED) {
                        itemStateTv?.text = "连接成功"
                    }

                }

                BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothA2dp.EXTRA_STATE,
                        BluetoothA2dp.STATE_NOT_PLAYING
                    )
                    when (state) {
                        BluetoothA2dp.STATE_PLAYING -> {
                            itemStateTv?.text = "正在播放"
                        }
                        BluetoothA2dp.STATE_NOT_PLAYING -> {
                            itemStateTv?.text = "播放停止"
                        }
                    }
                }
            }
        }

    }


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

    interface ConnectListener {
        fun onStart()
        fun onConnected()
        fun onFail(errorMsg: String)
    }

    override fun onDestroy() {
        super.onDestroy()
        BtBlueImpl.release()
        bluetooth.closeProfileProxy(BluetoothProfile.A2DP,bluetoothA2dp)
        connectThread?.cancel()
    }
}