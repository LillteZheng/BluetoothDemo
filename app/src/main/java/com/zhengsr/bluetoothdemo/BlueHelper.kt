package com.zhengsr.bluetoothdemo

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.zhengsr.bluetoothdemo.bt.blueImpl.BtBlueImpl
import java.util.*

/**
 * @author by zhengshaorui 2020/8/4 16:04
 * describe：蓝牙辅助类,提供已发现的数据
 */

object BlueHelper {

    private lateinit var blCollection: BtBlueImpl

    val BLUE_UUID = UUID.fromString("00001101-2300-1000-8000-00815F9B34FB")

    fun initBL(context: Context) = run {
        blCollection = BtBlueImpl(context)
        blCollection
    }


    fun getBl(): BtBlueImpl?{
        return blCollection
    }

    fun getBluetooth() : BluetoothAdapter{
        return BluetoothAdapter.getDefaultAdapter()
    }

    /**
     * 释放
     */
    fun release() {
        if (BlueHelper::blCollection.isInitialized){
            blCollection.release()
        }
    }

}

