package com.zhengsr.bluetoothdemo.utils

/**
 * @author by zhengshaorui 2020/8/27 10:31
 * describe：工具类，都用顶层函数
 */

/**
 * 数组转十六进制字符串
 */
internal fun bytesToHexString(src: ByteArray): String {
    val stringBuilder = StringBuilder("")
    for (element in src) {
        val v = element.toInt() and 0xFF
        val hv = Integer.toHexString(v)
        if (hv.length < 2) {
            stringBuilder.append(0)
        }
        stringBuilder.append(hv)
    }
    return stringBuilder.toString()
}

/**
 * 十六进制字符串转字符数组
 */
fun hexStringToBytes(hexString: String): ByteArray {
    var hexString = hexString
    hexString = hexString.toUpperCase()
    val length = hexString.length / 2
    val hexChars = hexString.toCharArray()
    val d = ByteArray(length)
    for (i in 0..length - 1) {
        val pos = i * 2
        d[i] = (charToByte(hexChars[pos]).toInt() shl 4 or charToByte(hexChars[pos + 1]).toInt()).toByte()
    }
    return d
}

/**
 * Convert char to byte
 * @param c char
 * *
 * @return byte
 */
private fun charToByte(c: Char): Byte {

    return "0123456789ABCDEF".indexOf(c).toByte()
}

