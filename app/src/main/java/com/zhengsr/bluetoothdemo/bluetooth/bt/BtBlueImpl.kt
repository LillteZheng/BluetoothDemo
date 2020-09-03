package com.zhengsr.bluetoothdemo.bluetooth.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

/**
 * @author by zhengshaorui 2020/8/5 09:26
 * describe：实现一些基本功能和接口
 */
typealias BlueDevFoundListener = (BluetoothDevice) -> Unit
typealias BlueBroadcastListener = (context: Context?, intent: Intent?) -> Unit


object BtBlueImpl {
    val BLUE_UUID = UUID.fromString("00001101-2300-1000-8000-00815F9B34FB")


    private var context: Context? = null
    fun init(context: Context?): BtBlueImpl {
        this.context = context?.applicationContext
        return this
    }

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private var blueBroadcastListener: BlueBroadcastListener? = null
    private var blueDevFoundListener: BlueDevFoundListener? = null

    private var blueBroadcast: BlueBroadcast? = null

    private val job = Job()
    private val scope = CoroutineScope(job)

    /**
     * 注册广播
     */
    fun registerBroadcast(
        actions: List<String>? = null,
        callback: BlueBroadcastListener? = null
    ): BtBlueImpl {
        blueBroadcastListener = callback
        IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            blueBroadcast = BlueBroadcast()
            context?.registerReceiver(blueBroadcast, this)
        }
        return this
    }

    /**
     * 蓝牙广播接收
     */
    class BlueBroadcast : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            blueBroadcastListener?.let { it(context, intent) }
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device ?: return
                    blueDevFoundListener?.let { it(device) }

                }

            }
        }

    }

    /**
     * 查找蓝牙
     */
    fun foundDevices(callback: BlueDevFoundListener?) {
        scope.launch {
            val time = System.currentTimeMillis()
            blueDevFoundListener = callback;
            //先取消搜索
            bluetoothAdapter.cancelDiscovery()

            //获取已经配对的设备
            val bondedDevices = bluetoothAdapter.bondedDevices
            bondedDevices?.forEach { device ->
                //公布给外面，方便 recyclerview 等设备连接
                callback?.let { it(device) }
            }
            val now = System.currentTimeMillis() - time;
            //搜索蓝牙，这个过程大概12s左右
            bluetoothAdapter.startDiscovery()

        }


    }

    /**
     * 注册需要的广播
     */
    // abstract fun initBroadcast(): IntentFilter?

    /**
     * 拿到蓝牙类
     */
    fun getBluetooth(): BluetoothAdapter {
        return bluetoothAdapter
    }

    /**
     * 释放资源
     */
    fun release() {
        blueBroadcast?.let { context?.unregisterReceiver(it) }
    }


}