package edu.gatech.ce.allgather.old.helpers

import android.content.Context
import android.util.Log
import edu.gatech.ce.allgather.AllGatherApplication
import edu.gatech.ce.allgather.old.base.MainActivity
import edu.gatech.ce.allgather.old.utils.StorageUtil
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by achatterjee36 on 6/14/2017.
 */

abstract class HelperClass(protected var context: Context) {

    var isRecording = false
    var strUtil: StorageUtil
    val mainActivity: MainActivity = context as MainActivity
    fun setIsRecording(b : Boolean)
    {
        isRecording = b
    }


    init {
        strUtil = (mainActivity.application as AllGatherApplication).storageUtil
    }

    public fun getMyStrUtil() : StorageUtil {return strUtil}

    // returns whether the HelperClass is ready to start recording or not
    abstract val isReady: Boolean

    //children will implement exactly how to record
    @Throws(IOException::class, FileNotFoundException::class)
    abstract fun record(f: File)

    //children will implement exactly how to record
    abstract fun stopRecord()

    //TODO: assumes external storage will be used
    //start recording. throw exception if this is called when isReady is false.
    @Throws(IllegalStateException::class, FileNotFoundException::class, IOException::class)
    open fun startRecording(timeStamp: Long) {
        if (isRecording || !StorageUtil.isExternalStorageWritable())
        {
            if(!isReady) Log.e("AC1","not ready")
            if(isRecording) Log.e("AC1","is recording")
            if(!StorageUtil.isExternalStorageWritable()) Log.e("AC1","ext storage problem")

            throw IllegalStateException()
        }

        else {
            //turn timestamp into a file name
            val dt = Date(timeStamp)
            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS")
            val f = sdf.format(dt)

            record(name2File(f))
            isRecording = true
        }
    }

    //stop recording.
    @Throws(IllegalStateException::class)
    fun stopRecording() {
        if (!isRecording)
            throw IllegalStateException("Already stopped recording!")
        else {
            isRecording = false
            stopRecord()
        }
    }

    //takes a file name and returns a File object inside the correct folder with correct extension
    protected abstract fun name2File(fileName: String): File

}
