package com.yetote.greendemo

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ired.student.mvp.live.green.Camera2SurfaceView
import hik.fp.baseline.port.common.util.LogcatHelper
import jp.co.cyberagent.android.gpuimage.filter.GPUImageChromaKeyBlendFilter

class MainActivity : AppCompatActivity() {
    private lateinit var surfaceView: ImageView
    lateinit var camera2SurfaceView: Camera2SurfaceView
    lateinit var btn: Button
    lateinit var inversionBtn: Button
    lateinit var changeBtn: Button
    lateinit var openBtn: Button
    lateinit var changeDataSource: Button
    lateinit var startPushBtn: Button
    lateinit var stopPushBtn: Button
    lateinit var saveBtn: Button
    lateinit var addBtn: Button
    lateinit var deleteBtn: Button
    lateinit var seekBar: SeekBar
    var index = 0
    val imageArr = mutableListOf(R.drawable.one, R.drawable.two, R.drawable.three)
    val imageArr2 = mutableListOf(R.drawable.four, R.drawable.five, R.drawable.six)
    val imageArr3 = mutableListOf(
        "https://img2.baidu.com/it/u=3073928000,614842258&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=889",
        "https://img2.baidu.com/it/u=682113864,1593348592&fm=253&fmt=auto&app=138&f=JPEG?w=285&h=500",
        "https://img0.baidu.com/it/u=3764515943,4106086017&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=889"
    )
    private val TAG = "MainActivity"
    val filter = GPUImageChromaKeyBlendFilter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val log = LogcatHelper.newInstance(this)
        Toast.makeText(this, " pathLogcat = ${this.getExternalFilesDir(null)!!.path}/logInfo/}", Toast.LENGTH_LONG).show()
        log!!.start()
        surfaceView = findViewById(R.id.surfaceView)
        camera2SurfaceView = Camera2SurfaceView(this, surfaceView)
        camera2SurfaceView.startPreview()
        seekBar = findViewById(R.id.seekbar)
        btn = findViewById(R.id.btn)
        changeBtn = findViewById(R.id.change)
        inversionBtn = findViewById(R.id.mirror)
        openBtn = findViewById(R.id.open)
        changeDataSource = findViewById(R.id.change_data_source)
        startPushBtn = findViewById(R.id.start_push)
        stopPushBtn = findViewById(R.id.stop_push)
        saveBtn = findViewById(R.id.save_bitmap)
        addBtn = findViewById(R.id.add_btn)
        deleteBtn = findViewById(R.id.delete_btn)


        addBtn.setOnClickListener {
            val p = seekBar.progress + 5
            seekBar.progress = p
        }
        deleteBtn.setOnClickListener {
            val p = seekBar.progress - 5
            seekBar.progress = p
        }
        camera2SurfaceView.changeGreenScreenBackgroundDataSource(imageArr)
        var dataS = 0
        btn.setOnClickListener {
            camera2SurfaceView.changeGreenScreenBackground()
        }
        inversionBtn.setOnClickListener {
            camera2SurfaceView.inversion()
        }
        changeBtn.setOnClickListener {
            camera2SurfaceView.switchCamera()
        }
        openBtn.setOnClickListener {
//            camera2SurfaceView.changeGreenScreen()
        }

        startPushBtn.setOnClickListener {
            camera2SurfaceView.startPush()
        }
        stopPushBtn.setOnClickListener {
            camera2SurfaceView.stopPush()
        }

        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Log.e(TAG, "onProgressChanged: progress=$progress" )
                camera2SurfaceView.setThresholdSensitivity(progress.toFloat() / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                Log.e(TAG, "onStartTrackingTouch: a")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Log.e(TAG, "onStartTrackingTouch: b")
            }

        })

        changeDataSource.setOnClickListener {
            val url = imageArr3[index]
            camera2SurfaceView.changeGreenScreenBackground(url)
            index++
            if (index >= 3) index = 0
//            if (dataS == 0) {
//                camera2SurfaceView.changeGreenScreenBackgroundDataSource(imageArr2)
//                camera2SurfaceView.changeGreenScreenBackground()
//                dataS = 1
//            } else {
//                camera2SurfaceView.changeGreenScreenBackgroundDataSource(imageArr)
//                camera2SurfaceView.changeGreenScreenBackground()
//                dataS = 0
//            }

        }
        saveBtn.setOnClickListener {
            camera2SurfaceView.isSave = true
        }
    }

    fun showLog(imageWidth: Int, imageHeight: Int, pixelStride: Int, rowStride: Int, rowPadding: Int, format: Int) {
        runOnUiThread {
            AlertDialog.Builder(this).setMessage(
                "imageWidth=$imageWidth \n" +
                        "imageHeight=$imageHeight \n" +
                        "pixelStride=$pixelStride \n" +
                        "rowStride=$rowStride \n" +
                        "rowPadding=$rowPadding \n" +
                        "format=${format} \n"
            ).create().show()
        }
    }
}
