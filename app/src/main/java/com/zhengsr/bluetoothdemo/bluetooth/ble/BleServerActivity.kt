package com.zhengsr.bluetoothdemo.bluetooth.ble

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.zhengsr.bluetoothdemo.R
import java.util.*


/**
 * @author zhengshaorui
 * 外围设备，会不断地发出广播，让中心设备知道，一旦连接上中心设备，就会停止发出广播
 * Android 5.0 之后，才能充当外围设备
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BleServerActivity : AppCompatActivity() {


    companion object{
        val UUID_SERVICE = UUID.fromString("10000000-0000-0000-0000-000000000000")
        val UUID_READ_NOTIFY = UUID.fromString("11000000-0000-0000-0000-000000000000")
        val UUID_WRITE = UUID.fromString("12000000-0000-0000-0000-000000000000")
        val UUID_DESCRIBE = UUID.fromString("12000000-0000-0000-0000-000000000000")
    }
    private val TAG = javaClass.simpleName

    private lateinit var textView: TextView
    private val mSb = StringBuilder()
    private var mBluetoothGattServer: BluetoothGattServer? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_server)
        textView = findViewById(R.id.info)
        initBle()
    }


    private fun initBle() {
        /**
         * GAP广播数据最长只能31个字节，包含两中： 广播数据和扫描回复
         * - 广播数据是必须的，外设需要不断发送广播，让中心设备知道
         * - 扫描回复是可选的，当中心设备扫描到才会扫描回复
         * 广播间隔越长，越省电
         */

        //广播设置
        val advSetting = AdvertiseSettings.Builder()
            //低延时，高功率，不使用后台
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            // 高的发送功率
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            // 可连接
            .setConnectable(true)
            //持续广播
            .setTimeout(0)
            .build()
        //设置广播包，这个是必须要设置的
        val advData = AdvertiseData.Builder()
            .setIncludeDeviceName(true) //显示名字
            .setIncludeTxPowerLevel(true)//设置功率
            // .addManufacturerData(1, byteArrayOf(23,33)) //设置厂商数据

            .build()


        //扫描相应数据（可不写，客户端扫描才发送）
       val msg =  "我是测试，我在测试"
        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(false) //不显示名字
            .setIncludeTxPowerLevel(false) //隐藏发射功率
            .addManufacturerData(2, byteArrayOf(23, 36)) //设置厂商数据
            .addServiceUuid(ParcelUuid(UUID_SERVICE)) //设置 UUID 服务的 uuid
            .build()


        /**
         * GATT 使用了 ATT 协议，ATT 把 service 和 characteristic 对应的数据保存在一个查询表中，
         * 依次查找每一项的索引
         * BLE 设备通过 Service 和 Characteristic 进行通信
         * 外设只能被一个中心设备连接，一旦连接，就会停止广播，断开又会重新发送
         * 但中心设备同时可以和多个外设连接
         * 他们之间需要双向通信的话，唯一的方式就是建立 GATT 连接
         * 外设作为 GATT(server)，它维持了 ATT 的查找表以及service 和 charateristic 的定义
         */

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        //开启广播,这个外设就开始发送广播了
        bluetoothAdapter?.bluetoothLeAdvertiser?.startAdvertising(
            advSetting,
            advData,
            scanResponse,
            advertiseCallback
        )


        /**
         * 添加 Gatt service 用来通信
         */

        //开启广播service，这样才能通信，包含一个或多个 characteristic ，每个service 都有一个 uuid
        val gattService =
            BluetoothGattService(UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)


        /**
         * characteristic 是最小的逻辑单元
         * 一个 characteristic 包含一个单一 value 变量 和 0-n个用来描述 characteristic 变量的
         * Descriptor。与 service 相似，每个 characteristic 用 16bit或者32bit的uuid作为标识
         * 实际的通信中，也是通过 Characteristic 进行读写通信的
         */
        //添加读+通知的 GattCharacteristic
        val readCharacteristic = BluetoothGattCharacteristic(
            UUID_READ_NOTIFY,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )


        //添加写的 GattCharacteristic
        val writeCharacteristic = BluetoothGattCharacteristic(
            UUID_WRITE,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        //添加 Descriptor 描述符
        val descriptor =
            BluetoothGattDescriptor(UUID_DESCRIBE, BluetoothGattDescriptor.PERMISSION_WRITE)

        gattService.addCharacteristic(readCharacteristic)
        gattService.addCharacteristic(writeCharacteristic)

        //为特征值添加描述
        writeCharacteristic.addDescriptor(descriptor)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        //打开 GATT 服务，方便客户端连接
        mBluetoothGattServer = bluetoothManager.openGattServer(this, gattServiceCallbak)
        mBluetoothGattServer?.addService(gattService)


    }

    private val gattServiceCallbak = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            device ?: return
            Log.d(TAG, "zsr onConnectionStateChange: ")
            if (status == BluetoothGatt.GATT_SUCCESS && newState == 2) {
                logInfo("连接到中心设备: ${device?.name}")
            } else {
                logInfo("与: ${device?.name} 断开连接失败！")
            }
        }


        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)

            /**
             * 中心设备read时，回调
             */
            val data = "this is a test"
            mBluetoothGattServer?.sendResponse(
                device, requestId, BluetoothGatt.GATT_SUCCESS,
                offset, data.toByteArray()
            )
            logInfo("客户端读取 [characteristic ${characteristic?.uuid}] $data")
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
            mBluetoothGattServer?.sendResponse(
                device, requestId, BluetoothGatt.GATT_SUCCESS,
                offset, value
            )
            value?.let {
                logInfo("客户端写入 [characteristic ${characteristic?.uuid}] ${String(it)}")
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            val data = "this is a test"
            mBluetoothGattServer?.sendResponse(
                device, requestId, BluetoothGatt.GATT_SUCCESS,
                offset, data.toByteArray()
            )
            logInfo("客户端读取 [descriptor ${descriptor?.uuid}] $data")
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(
                device,
                requestId,
                descriptor,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )

            value?.let {
                logInfo("客户端写入 [descriptor ${descriptor?.uuid}] ${String(it)}")
                // 简单模拟通知客户端Characteristic变化
                Log.d(TAG, "zsr onDescriptorWriteRequest: $value")
            }


        }

        override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
            super.onExecuteWrite(device, requestId, execute)
            Log.d(TAG, "zsr onExecuteWrite: ")
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)
            Log.d(TAG, "zsr onNotificationSent: ")
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            super.onMtuChanged(device, mtu)
            Log.d(TAG, "zsr onMtuChanged: ")
        }
    }


    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            logInfo("服务准备就绪，请搜索广播")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            logInfo("服务启动失败: $errorCode")
        }
    }

    private fun logInfo(msg: String) {
        runOnUiThread {
            mSb.apply {
                append(msg).append("\n")
                textView.text = toString()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothAdapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        mBluetoothGattServer?.close()
    }
}