package com.zhengsr.bluetoothdemo.bluetooth.ble

import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.listener.OnItemClickListener
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.zhengsr.bluetoothdemo.R
import java.util.*

/**
 * @author zhengshaorui
 * 中心设备，可以扫描到多个外围设备，并从外围设备获取信息
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class BleClientActivity : AppCompatActivity(), OnItemClickListener {


    val handler = Handler(Looper.getMainLooper())
    private var mScanning: Boolean = false;
    private var mBleAdapter: BlueAdapter? = null
    private val mData: MutableList<BleData> = mutableListOf();
    private var mBluetoothGatt: BluetoothGatt? = null
    private val mSb = StringBuilder()
    private lateinit var mInfoTv: TextView;
    private var bluetoothAdapter: BluetoothAdapter? = null;
    private var blueGatt: BluetoothGatt? = null
    private var isConnected = false
    private lateinit var editText: EditText
    companion object {
        private val TAG = "BleClientActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_client)
        mInfoTv = findViewById(R.id.info_tv)
        editText = findViewById(R.id.edit)
        initRecyclerView()

        //是否支持低功耗蓝牙
        initBluetooth()
    }



    private fun initBluetooth() {
        packageManager.takeIf { !it.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }
            ?.let {
                Toast.makeText(this, "您的设备没有低功耗蓝牙驱动！", Toast.LENGTH_SHORT).show()
                finish()
            }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    /**
     * 初始化 recyclerview
     */
    private fun initRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler)
        val manager = LinearLayoutManager(this)
        recyclerView.layoutManager = manager
        mBleAdapter = BlueAdapter(R.layout.recy_ble_item_layout, mData)
        recyclerView.adapter = mBleAdapter

        mBleAdapter?.setOnItemClickListener(this)
    }

    class BlueAdapter(layoutId: Int, datas: MutableList<BleData>) :
        BaseQuickAdapter<BleData, BaseViewHolder>(layoutId, datas) {
        override fun convert(holder: BaseViewHolder, item: BleData) {
            //没有名字不显示
            holder.setText(R.id.item_ble_name_tv, "名称: " + item.dev.name ?: "Null")
                .setText(R.id.item_ble_mac_tv, "地址: " + item.dev.address)
                .setText(R.id.item_ble_device_tv, item.scanRecord)
        }

    }

    override fun onItemClick(adapter: BaseQuickAdapter<*, *>, view: View, position: Int) {
        //连接之前先关闭连接
        closeConnect()
        val bleData = mData[position]
        blueGatt = bleData.dev.connectGatt(this, false, blueGattListener)
        logInfo("开始与 ${bleData.dev.name} 连接.... $blueGatt")
    }


    /**
     * 断开连接
     */
    private fun closeConnect() {
        BleBlueImpl.stopScan()
        blueGatt?.let {
            it.disconnect()
            it.close()
        }

    }


    private val blueGattListener = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            val device = gatt?.device
            if (newState == BluetoothProfile.STATE_CONNECTED){
                isConnected = true
                //开始发现服务，有个小延时，最后200ms后尝试发现服务
                handler.postDelayed({
                    gatt?.discoverServices()
                },300)

                device?.let{logInfo("与 ${it.name} 连接成功!!!")}
            }else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                isConnected = false
                logInfo("无法与 ${device?.name} 连接: $status")
                closeConnect()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
           // Log.d(TAG, "zsr onServicesDiscovered: ${gatt?.device?.name}")
            val service = gatt?.getService(BleBlueImpl.UUID_SERVICE)
            mBluetoothGatt = gatt
            logInfo("已连接上 GATT 服务，可以通信! ")

            /*if (status == BluetoothGatt.GATT_SUCCESS){
                gatt?.services?.forEach {service ->
                    logInfo("service 的 uuid: ${service.uuid}")
                    service.characteristics.forEach{ characteristic ->
                        logInfo("characteristic 的 uuid: ${characteristic.uuid}")
                        characteristic.descriptors.forEach { descrip ->
                            logInfo("descrip 的 uuid: ${descrip.uuid}")
                        }
                    }
                }
            }*/
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            characteristic?.let {
                val data = String(it.value)
                logInfo("CharacteristicRead 数据: $data")
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            characteristic?.let {
                val data = String(it.value)
                logInfo("CharacteristicWrite 数据: $data")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            characteristic?.let {
                val data = String(it.value)
                logInfo("CharacteristicChanged 数据: $data")
            }
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorRead(gatt, descriptor, status)
            descriptor?.let {
                val data = String(it.value)
                logInfo("DescriptorRead 数据: $data")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            descriptor?.let {
                val data = String(it.value)
                logInfo("DescriptorWrite 数据: $data")
            }
        }
    }


    /**
     * 扫描
     */
    fun scan(view: View) {
        mData.clear()
        mBleAdapter?.notifyDataSetChanged()
        BleBlueImpl.scanDev {dev ->
            dev.dev.name?.let {
                if (dev !in mData) {
                    mData.add(dev)
                    mBleAdapter?.notifyItemInserted(mData.size)
                }
            }
        }
    }



    val sb  = StringBuilder()
    private fun logInfo(msg:String){
        runOnUiThread {
            sb.apply {
                append(msg).append("\n")
                mInfoTv.text = toString()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        closeConnect()
    }

    /**
     * 读数据
     */
    fun readData(view: View) {
        //找到 gatt 服务
        val service = getGattService(BleBlueImpl.UUID_SERVICE)
        if (service != null) {
            val characteristic =
                service.getCharacteristic(BleBlueImpl.UUID_READ_NOTIFY) //通过UUID获取可读的Characteristic
            mBluetoothGatt?.readCharacteristic(characteristic)
        }
    }


    // 获取Gatt服务
    private fun getGattService(uuid: UUID): BluetoothGattService? {
        if (!isConnected) {
            Toast.makeText(this, "没有连接", Toast.LENGTH_SHORT).show()
            return null
        }
        val service = mBluetoothGatt?.getService(uuid)
        if (service == null) {
            Toast.makeText(this, "没有找到服务", Toast.LENGTH_SHORT).show()
        }
        return service
    }

    fun writeData(view: View) {
        val msg = editText.text.toString()
        val service = getGattService(BleBlueImpl.UUID_SERVICE)
        if (service != null) {
            val characteristic =
                service.getCharacteristic(BleBlueImpl.UUID_WRITE) //通过UUID获取可读的Characteristic
            characteristic.value = msg.toByteArray()
            mBluetoothGatt?.writeCharacteristic(characteristic)
        }
    }

}