package com.yetote.greendemo

import com.tencent.trtc.TRTCCloud
import com.tencent.trtc.TRTCCloudDef
import com.tencent.trtc.TRTCCloudDef.TRTCParams
import com.tencent.trtc.TRTCCloudDef.TRTCVideoFrame
import java.util.concurrent.ArrayBlockingQueue

var width = 0
var height = 0
var dataQueue = ArrayBlockingQueue<ByteArray?>(30)
var isPush = false
fun pushData(data: ByteArray?) {
    try {
        dataQueue.put(data)
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
}

fun startPush() {
    isPush = true
    init()
    push()
}

fun stopPush() {
    isPush = false
    clearFrame()
}

private fun clearFrame() {
    dataQueue.clear()
}

fun getData(): ByteArray? {
    try {
        return dataQueue.take()
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
    return null
}

fun setWidthAndHeight(_width: Int, _height: Int) {
    width = _width
    height = _height
}

var mTRTCCloud: TRTCCloud? = null
var videoFrame: TRTCVideoFrame? = null

fun init() {
    mTRTCCloud = TRTCCloud.sharedInstance(getContext())
    mTRTCCloud!!.enableCustomVideoCapture(TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG, true)
    val param = TRTCParams()
    param.sdkAppId = 1400762661 // Please replace with your own SDKAppID
    param.userId = "yetote" // Please replace with your own userid
    param.roomId = 503779938 // Please replace with your own room number
    param.userSig =
        "eJw1zEELgjAYxvHvsnPIu*VmCt06BHkQCipvwt7yJdqGDp1G3z3TOj6-PzwvdsqPUYcNy5iIgK3mTRqNpxvNPKC3Hn*l1Y-KOdIs4zFAooRSfCkYHDU4uZRSAMCinp5fUypJOU--2tJ9Oq6rmJfXYd-rkDhj*w0FLBzsLmVxHtXYWWGgsXG*PsCWvT*WcjKF" // Please replace with your own userSig
    param.role = TRTCCloudDef.TRTCRoleAnchor
    mTRTCCloud!!.enterRoom(param, TRTCCloudDef.TRTC_APP_SCENE_LIVE)
}

fun push() {
    Thread {
        while (isPush) {
            val trtcVideoFrame = TRTCVideoFrame()
            trtcVideoFrame.data = getData()
            trtcVideoFrame.bufferType = TRTCCloudDef.TRTC_VIDEO_BUFFER_TYPE_BYTE_ARRAY
            trtcVideoFrame.pixelFormat = TRTCCloudDef.TRTC_VIDEO_PIXEL_FORMAT_NV21
            trtcVideoFrame.width = width
            trtcVideoFrame.height = height
            mTRTCCloud!!.sendCustomVideoData(trtcVideoFrame)
        }
    }.start()
}