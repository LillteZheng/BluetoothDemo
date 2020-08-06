package com.zhengsr.bluetoothdemo.bt.blueImpl

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.IntentFilter

/**
 * @author by zhengshaorui 2020/8/4 17:06
 * describe：
 */

class BtBlueImpl internal constructor(context: Context) : BaseBlueImpl(context) {
    companion object {
        private val TAG = javaClass.simpleName
    }


    /**
     * 注册广播
     */
    override fun initBroadcast(): IntentFilter? {

        return IntentFilter().run {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            this
        }


    }


}