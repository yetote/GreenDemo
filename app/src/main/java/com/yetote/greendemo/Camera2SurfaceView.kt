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
     * ?????????????????????
     * @param ratio 1??????16???9 ??????????????????4???3 ???????????????????????????16???9
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
                val matrix = Matrix() // ????????????????????????????????????
                matrix.postScale(-1f, 1f) // ???????????????????????????
                // ???????????????????????????????????????
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
     * ?????????????????????
     * @param open true ???????????? false ????????????
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
     * ???????????????????????????
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
     * ???????????????????????????
     * @param list ?????????????????????
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
     * ?????????????????? url??????
     * @param url ?????????url
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
     * ?????????????????? url??????
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
     * ?????????????????? url??????
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
     * ??????????????????
     * @param _index ?????????????????????????????????????????????????????????????????????????????????????????????
     *               ????????????????????????????????????????????????0
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
     * ??????
     */
    fun inversion() {
        isInversion = !isInversion
    }

    /**
     * Start push
     * ????????????
     */
    fun startPush() {
        isPush = true
//        com.ired.student.mvp.live.green.startPush()
    }

    /**
     * Stop push
     * ????????????
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
