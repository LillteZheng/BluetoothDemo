package com.zhengsr.bluetoothdemo.utils

import java.io.Closeable

/**
 * @author by zhengshaorui 2020/7/29 17:26
 * describe：关闭类，支持 closeable
 */

fun close(vararg closeable:Closeable?){
    closeable?.forEach {
        obj ->
        obj?.close()
    }
}