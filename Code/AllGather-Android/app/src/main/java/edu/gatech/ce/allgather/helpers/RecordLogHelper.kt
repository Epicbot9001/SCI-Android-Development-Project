package edu.gatech.ce.allgather.helpers

import android.util.Log
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.utils.AppUtils
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Record Log Helper
 * @author Justin Lee
 * @date 2020/6/15
 */
object RecordLogHelper {

    @Volatile
    private var canLog = false

    /**
     * prepare logging
     *
     * Created by Justin Lee
     */
    fun prepareLog(formatString: String) {
        if (!canLog) {
            val sb = StringBuilder()
            sb.append("[")
            sb.append(formatString)
            sb.append("] { message=\"Started recording\", ")
            //append driver id
            sb.append("driverId=\"${AppUtils.deviceID}\", ")
            sb.append("IMEI=\"${AppUtils.IMEI}\", ")
            //append data version
            sb.append("dataVersion=\"${AppUtils.appContext.getString(R.string.data_version)}\"")
            sb.append("}\n")
            //write
            val folder = File(StorageHelper.defaultStoragePath)
            if (!folder.mkdirs()) {
                Log.d("AC1", "Main Directory not created")
            }
            try {
                val fos = FileOutputStream(File(folder, "log_${AppUtils.IMEI}.txt"), true)
                fos.write(sb.toString().toByteArray())
                fos.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                canLog = !canLog
            }
        }
    }

    /**
     * End Log
     *
     * Created by Justin Lee
     */
    fun endLog() {
        if (canLog) {
            //build string to write
            //append timestamp
            val timestamp = System.currentTimeMillis()
            val dt = Date(timestamp)
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
            val sb = StringBuilder()
            sb.append("[")
            sb.append(sdf.format(dt))
            sb.append("] { message=\"Ended recording\" }\n\n")
            //write
            val folder = File(StorageHelper.defaultStoragePath)
            if (!folder.mkdirs()) {
                Log.d("AC1", "Directory not created")
            }
            try {
                val fos = FileOutputStream(File(folder, "log_${AppUtils.IMEI}.txt"), true)
                fos.write(sb.toString().toByteArray())
                fos.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                canLog = !canLog
            }
        }
    }

    /**
     * logging
     *
     * @param ex logging
     * Created by Justin Lee
     */
    fun log(ex: Exception) {
        if (canLog) {
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            ex.printStackTrace(printWriter)
            val stackTraceString = stringWriter.toString()
            val timestamp = System.currentTimeMillis()
            val date = Date(timestamp)

            //write
            val folder = File(StorageHelper.defaultStoragePath)
            if (!folder.mkdirs()) {
                Log.d("AC1", "Directory not created")
            }
            try {
                val fos = FileOutputStream(File(folder, "log_${AppUtils.IMEI}.txt"), true)
                fos.write("Exception thrown at $date".toByteArray())
                fos.write(stackTraceString.toByteArray())
                fos.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}