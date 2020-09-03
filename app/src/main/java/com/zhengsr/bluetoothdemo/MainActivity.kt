package com.zhengsr.bluetoothdemo

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zhengsr.bluetoothdemo.bluetooth.a2dp.A2dpActivity
import com.zhengsr.bluetoothdemo.bluetooth.ble.BleClientActivity
import com.zhengsr.bluetoothdemo.bluetooth.ble.BleServerActivity
import com.zhengsr.bluetoothdemo.bluetooth.bt.BtClientActivity
import com.zhengsr.bluetoothdemo.bluetooth.bt.BtServerActivity
import com.zhengsr.zplib.ZPermission

const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission()

        val bluetooth = BluetoothAdapter.getDefaultAdapter()
        if (bluetooth == null) {
            Toast.makeText(this, "您的设备未找到蓝牙驱动！!", Toast.LENGTH_SHORT).show()
            finish()
        }else {
            if (!bluetooth.isEnabled) {
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1){
            if (resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(this, "请您不要拒绝开启蓝牙，否则应用无法运行", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun requestPermission(){

        ZPermission.with(this)
                .permissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_ADMIN
                ).request { isAllGranted, deniedLists ->
                    if (!isAllGranted){
                        Toast.makeText(this, "需要开启权限才能运行应用", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }


        //在 Android 10 还需要开启 gps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val lm: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                Toast.makeText(this@MainActivity, "请您先开启gps,否则蓝牙不可用", Toast.LENGTH_SHORT).show()
            }
        }

    }

    fun client(view: View) {startActivity(Intent(this,
        BtClientActivity::class.java))}
    fun server(view: View) {startActivity(Intent(this,
        BtServerActivity::class.java))}

    fun a2dpclient(view: View) {startActivity(Intent(this,
        A2dpActivity::class.java))}

    fun bleServer(view: View) {startActivity(Intent(this,BleServerActivity::class.java))}
    fun bleClient(view: View) {startActivity(Intent(this,BleClientActivity::class.java))}
}