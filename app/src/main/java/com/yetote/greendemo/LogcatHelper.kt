package hik.fp.baseline.port.common.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * @Description: 把日志写到本地
 * @date 2020/12/23
 */
class LogcatHelper private constructor(context: Context) {
    var cmds: String? = null
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss") //设置日期格式
    private var logcatProc: Process? = null
    private var mLogDumper: LogDumper? = null
    private val mPid: Int

    init {
        init(context)
        mPid = android.os.Process.myPid()
    }

    /**
     * 初始化目录
     */
    private fun init(context: Context) {

//        String path = "";
//        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
//            //保存在SD卡中
//            pathLogcat = Environment.getExternalStorageDirectory().path + "/logInfo/"
//        } else {
//            //保存到应用目录下 todo 需要测试
//            pathLogcat = context.filesDir.absolutePath + "/logInfo/"
//        }
        pathLogcat = context.getExternalFilesDir(null)!!.path + "/logInfo/"
        val file = File(pathLogcat)
        if (!file.exists()) {
            file.mkdirs()
            Log.e(TAG, "创建文件夹")
        }
        Log.e(TAG, pathLogcat.toString())
    }

    fun start() {
        if (mLogDumper == null) {
            mLogDumper = LogDumper(mPid.toString(), pathLogcat)
        }
        mLogDumper!!.start()
    }

    fun stop() {
        if (mLogDumper != null) {
            mLogDumper!!.stopLogs()
            mLogDumper = null
        }
    }

    private inner class LogDumper(private val mPid: String, dir: String?) : Thread() {
        private var outputStream: FileOutputStream? = null
        private var mReader: BufferedReader? = null
        private var mIsRunning = true

        init {
            val timeMillis = System.currentTimeMillis()

            //错误日志文件名称
            val fileName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                "hikvison-log${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}.log"
            } else {
                val time: String = sdf.format(Date())
                "hikvison-log$time.log"
            }
            try {
                outputStream = FileOutputStream(File(dir, fileName))
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            //显示当前mPid程序的日志等级  日志等级：*:v , *:d , *:w , *:e , *:f , *:s
            // cmds = "logcat *:e *:w | grep \"(" + mPid + ")\"";
            cmds = "logcat  | grep \"($mPid)\"\n" //打印所有日志信息
            // cmds = "logcat -s way";//打印标签过滤信息
//            cmds = "logcat *:e *:i | grep \"(" + mPid + ")\"";
        }

        fun stopLogs() {
            mIsRunning = false
        }

        override fun run() {
            try {
                logcatProc = Runtime.getRuntime().exec(cmds)
                mReader = BufferedReader(InputStreamReader(logcatProc?.inputStream), 1024)
                var line: String? = null
                while (mIsRunning && mReader!!.readLine().also { line = it } != null) {
                    if (!mIsRunning) {
                        break
                    }
                    if (line!!.length == 0) {
                        continue
                    }
                    if (outputStream != null && line!!.contains(mPid)) {
                        outputStream!!.write("""$line""".toByteArray())
                        outputStream!!.write(System.getProperty("line.separator").toByteArray())
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (logcatProc != null) {
                    logcatProc!!.destroy()
                    logcatProc = null
                }
                if (mReader != null) {
                    try {
                        mReader!!.close()
                        mReader = null
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream!!.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    outputStream = null
                }
            }
        }
    }

    companion object {
        const val TAG = "LogcatHelper"
        private var pathLogcat: String? = null

        @Volatile
        private var mInstance: LogcatHelper? = null
        fun newInstance(context: Context): LogcatHelper? {
            if (mInstance == null) {
                synchronized(LogcatHelper::class.java) {
                    if (mInstance == null) {
                        mInstance = LogcatHelper(context)
                    }
                }
            }
            return mInstance
        }
    }
}