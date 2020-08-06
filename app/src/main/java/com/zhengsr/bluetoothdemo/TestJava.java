package com.zhengsr.bluetoothdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author by zhengshaorui 2020/8/4 17:17
 * describe：
 */
public class TestJava {
    private int test2;
    class test extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            test2 = 0;
        }
    }
}
