package com.yetote.greendemo

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.FileOutputStream

fun getContext(): Context? {
    var context: Context? = null
    try {
        val activityThread = Class.forName("android.app.ActivityThread")
        val currentApplicationMethod = activityThread.getDeclaredMethod("currentApplication")
        context = currentApplicationMethod.invoke(null as Any?) as Context
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return context
}

fun getNV21(inputWidth: Int, inputHeight: Int, scaled: Bitmap): ByteArray? {
    var inputHeight = inputHeight
    if (inputHeight % 2 != 0) inputHeight -= 1
    val argb = IntArray(inputWidth * inputHeight)
    scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight)
//    scaled.recycle()
    val yuv = ByteArray(inputWidth * inputHeight * 3 / 2)
    encodeYUV420SP(yuv, argb, inputWidth, inputHeight)

    return yuv
}

fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
    val frameSize = width * height
    var yIndex = 0
    var uvIndex = frameSize
    var a: Int
    var R: Int
    var G: Int
    var B: Int
    var Y: Int
    var U: Int
    var V: Int
    var index = 0
    for (j in 0 until height) {
        for (i in 0 until width) {
            a = argb[index] and -0x1000000 shr 24 // a is not used obviously
            R = argb[index] and 0xff0000 shr 16
            G = argb[index] and 0xff00 shr 8
            B = argb[index] and 0xff shr 0
            Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
            U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
            V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128
            yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
            if (j % 2 == 0 && index % 2 == 0) {
                yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
            }
            index++
        }
    }
}

fun bitmap2Path(bitmap: Bitmap, path: String): String {
    try {
        val os = FileOutputStream(path);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
        os.flush();
        os.close();
    } catch (e: Exception) {
        Log.e("TAG", "", e);
    }
    return path
}

