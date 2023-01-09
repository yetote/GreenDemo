package com.ired.student.mvp.live.green

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio.RATIO_16_9
import androidx.camera.core.AspectRatio.RATIO_4_3
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import com.yetote.greendemo.*
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageChromaKeyBlendFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (bitmap: Bitmap) -> Unit

class Camera2SurfaceView(val activity: AppCompatActivity, val surfaceView: ImageView) {
    private var resultBitmap: Bitmap? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var cameraProvider: ProcessCameraProvider
    private var isInversion: Boolean = false
    private val TAG = "Camera2SurfaceView"
    var gpuImage: GPUImage = GPUImage(getContext())
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isShowGreenScreen = false
    private val backgroundList = mutableListOf<Int>()
    val filter = MyGPUImageChromaKeyBlendFilter()
    private var index = 0
    private var path = ""
    private var isPush = false
    var isSave: Boolean = false
    var saveArray = true

    init {
        Log.e(TAG, "startPreview: ")
        filter.setColorToReplace(0f, 1f, 0f)
        filter.setThresholdSensitivity(0.4f)
    }

    /**
     * Start preview
     * 开启相机预览，
     * @param ratio 1表示16：9 其他数值表示4：3 不传该参数默认使用16：9
     */
    @SuppressLint("RestrictedApi")
    @JvmOverloads
    fun startPreview(ratio: Int = 1) {
        path = getContext()!!.externalCacheDir!!.path + "/res/output.yuv"
        if (!this::cameraExecutor.isInitialized) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(getContext()!!)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(if (ratio == 1) RATIO_16_9 else RATIO_4_3)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetAspectRatio(if (ratio == 1) RATIO_16_9 else RATIO_4_3)
                .setOutputImageRotationEnabled(true)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                    })
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(activity, cameraSelector, imageCapture, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(getContext()!!))
    }


    inner class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        @SuppressLint("UnsafeOptInUsageError")
        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val planes = image.planes
            val imageWidth = image.width
            val imageHeight = image.height
            val pixelStride: Int = planes.get(0).pixelStride
            val rowStride: Int = planes.get(0).rowStride
//            Log.e(TAG, "analyze: imageWidth=$imageWidth imageHeight=$imageHeight")
            val rowPadding: Int = rowStride - pixelStride * imageWidth
            val data = buffer.toByteArray()
            if (saveArray) {
                val activity = activity as MainActivity
                activity.showLog(imageWidth, imageHeight, pixelStride, rowStride, rowPadding, image.format)
                saveArray = false
            }
//            Log.e(TAG, "analyze:imageWidth=$imageWidth ")
//            Log.e(TAG, "analyze:imageHeight=$imageHeight ")
//            Log.e(TAG, "analyze:pixelStride=$pixelStride ")
//            Log.e(TAG, "analyze:rowStride=$rowStride ")
//            Log.e(TAG, "analyze:rowPadding=$rowPadding ")
//            Log.e(TAG, "analyze:format=${image.format} ")

            val bitmap = Bitmap.createBitmap(imageWidth + rowPadding / pixelStride, imageHeight, Bitmap.Config.ARGB_8888)
            var inversionBitmap: Bitmap? = null
            buffer.position(0)
            bitmap.copyPixelsFromBuffer(buffer)
            val lastBitmap = Bitmap.createBitmap(bitmap, 0, 0, imageWidth, imageHeight)
            bitmap.recycle()
            if (isSave) {
                ImageSaveUtil.saveBitmap2file(bitmap, getContext())
                Log.e(TAG, "analyze: save1")
            }
            if (isInversion) {
                val matrix = Matrix() // 创建操作图片用的矩阵对象
                matrix.postScale(-1f, 1f) // 执行图片的旋转动作
                // 创建并返回旋转后的位图对象
                inversionBitmap = Bitmap.createBitmap(
                    lastBitmap, 0, 0, lastBitmap.width,
                    lastBitmap.height, matrix, false
                )
                if (isSave) {
                    ImageSaveUtil.saveBitmap2file(inversionBitmap, getContext())
                }
            }
            if (isSave) {
                ImageSaveUtil.saveBitmap2file(lastBitmap, getContext())
                Log.e(TAG, "analyze: save1")
            }

//            Log.e(TAG, "analyze: width=$imageWidth,height=$imageHeight")
//            ImageSaveUtil.saveBitmap2file(bitmap, getContext()!!)
            gpuImage.setImage(if (isInversion) inversionBitmap else lastBitmap)
            gpuImage.requestRender()
            resultBitmap = null
            resultBitmap = gpuImage.bitmapWithFilterApplied
            activity.runOnUiThread {
                surfaceView.setImageBitmap(resultBitmap)
            }
            setWidthAndHeight(resultBitmap!!.width, resultBitmap!!.height)
            if (isPush) {
                pushData(getNV21(resultBitmap!!.width, resultBitmap!!.height, resultBitmap!!))
            }
            if (isSave) {
                ImageSaveUtil.saveBitmap2file(resultBitmap, getContext())
                Log.e(TAG, "analyze: save2")
            }
            image.close()
//            bitmap.recycle()
            lastBitmap.recycle()
            inversionBitmap?.recycle()
            if (isSave) {
                isSave = false
            }
        }

    }

    /**
     * Change green screen
     * 开启或关闭绿幕
     * @param open true 开启绿幕 false 关闭绿幕
     */
    fun changeGreenScreen(open: Boolean) {
        Log.e(TAG, "changeGreenScreen: index=$index")
        isShowGreenScreen = open
        if (!open) {
            gpuImage.setFilter(GPUImageFilter(GPUImageFilter.NO_FILTER_VERTEX_SHADER, GPUImageFilter.NO_FILTER_FRAGMENT_SHADER))
            gpuImage.requestRender()
        } else {
            gpuImage.setFilter(filter)
            gpuImage.requestRender()
        }
    }

    /**
     * Switch camera
     * 切换前置或后置相机
     */
    fun switchCamera() {
        cameraProvider.unbindAll()
        if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            startPreview()
        } else {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            startPreview()
        }
    }

    /**
     * Change green screen background data source
     * 切换绿幕背景数据源
     * @param list 绿幕背景数据源
     */
    fun changeGreenScreenBackgroundDataSource(list: MutableList<Int>) {
        Log.e(TAG, "changeGreenScreenBackgroundDataSource: ")
        backgroundList.clear()
        backgroundList.addAll(list)
        index = -1
        changeGreenScreenBackground()
    }

    /**
     * Change green screen background
     * 切换绿幕背景 url方式
     * @param url 图片的url
     */
    fun changeGreenScreenBackground(url: String) = GlobalScope.launch(Dispatchers.IO) {
        Log.e(TAG, "changeGreenScreenBackground: $url ")
        val imgUrl = URL(url)
        val conn = imgUrl
            .openConnection()
        conn.doInput = true
        conn.connect()
        val inputStream = conn.getInputStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        filter.bitmap = bitmap
        gpuImage.setFilter(filter)
        gpuImage.requestRender()
        isShowGreenScreen = true
    }

    /**
     * Change green screen background
     * 切换绿幕背景 url方式
     * @param iv imageview
     */
    fun changeGreenScreenBackground(iv: ImageView) {

        filter.bitmap = iv.drawToBitmap()
        gpuImage.setFilter(filter)
        gpuImage.requestRender()
        isShowGreenScreen = true
    }

    /**
     * Change green screen background
     * 切换绿幕背景 url方式
     * @param bitmap bitmap
     */
    fun changeGreenScreenBackground(bitmap: Bitmap) {
        filter.bitmap = bitmap
        gpuImage.setFilter(filter)
        gpuImage.requestRender()
        isShowGreenScreen = true
    }

    /**
     * Change green screen background
     * 切换绿幕背景
     * @param _index 索引，如果不传该参数，则默认切换当前背景数据源中的下一张图片，
     *               超过数据源最大索引，则索引重置为0
     */
    fun changeGreenScreenBackground(_index: Int = -1) {
        Log.e(TAG, "changeGreenScreenBackground: ")
        if (_index == -1) {
            index += 1
            Log.e(TAG, "changeGreenScreenBackground: +++++++++++++++++++++++")
        } else {
            index = _index
        }

        if (index >= backgroundList.size || index < 0) index = 0

        filter.bitmap = BitmapFactory.decodeResource(getContext()!!.resources, backgroundList[index])
        gpuImage.setFilter(filter)
        gpuImage.requestRender()
        isShowGreenScreen = true
    }

    /**
     * Inversion
     * 镜像
     */
    fun inversion() {
        isInversion = !isInversion
    }

    /**
     * Start push
     * 开始推流
     */
    fun startPush() {
        isPush = true
//        com.ired.student.mvp.live.green.startPush()
    }

    /**
     * Stop push
     * 结束推流
     */
    fun stopPush() {
        isPush = false
//        com.ired.student.mvp.live.green.stopPush()
    }

    fun setThresholdSensitivity(fl: Float) {
        filter.setThresholdSensitivity(fl)
        gpuImage.setFilter(filter)
        gpuImage.requestRender()
    }
}
