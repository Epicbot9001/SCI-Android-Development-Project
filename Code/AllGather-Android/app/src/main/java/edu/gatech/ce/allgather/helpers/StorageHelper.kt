package edu.gatech.ce.allgather.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import edu.gatech.ce.allgather.AllGatherApplication
import edu.gatech.ce.allgather.utils.AppUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Storage helper
 * @author Justin Lee
 * @date 2020/6/11
 */
@SuppressLint("StaticFieldLeak")
object StorageHelper {

    private var context: Context = AllGatherApplication.instance.applicationContext


    @RequiresApi(Build.VERSION_CODES.O)
    fun copyCalibration(calFolder: File) {

        val calFile = "$defaultStoragePath$calibration"
        val files = mutableListOf<String>()
        if (File(calFile).isDirectory) {
            Files.walk(Paths.get(calFile)).use { paths ->
                paths.filter {
                    Files.isRegularFile(it) and it.toString().endsWith("calibration.csv")}.forEach {
                    files.add(it.toString())
                }
            }
        }
        files.sort()
        if (files.size == 0) {return}

        val mostRecent = File(files.last())
        val mostRecentName = files.last().substring(files.last().lastIndexOf("/")+1)
        mostRecent.copyTo(File(calFolder, mostRecentName))
    }

    enum class StorageType(val type: String) {
        PHONE("Internal Storage"),
        SD("SD Card")
    }

    /** Checks if external storage is available for read and write */
    val isExternalStorageWritable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return state == Environment.MEDIA_MOUNTED
        }

    /** Checks if external storage is available to at least read */
    val isExternalStorageReadable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
        }

    /**
     * Possible Storage Paths
     *
     * key = Type
     * value = File path
     */
    private val possibleStoragePaths: LinkedHashMap<StorageType, String>
        get() {
            val storages = context.getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS)
            val storageList = LinkedHashMap<StorageType, String>()
            // The first index is the internal storage, second is the SD card
            storages.forEachIndexed { index, file ->
                if (index == 0) {
                    if (file != null)
                        storageList[StorageType.PHONE] = file.absolutePath + "/"
                } else {
                    if (file != null)
                        storageList[StorageType.SD] = file.absolutePath + "/"
                }
            }
            return storageList
        }

    /**
     * Default storage path
     */
    val defaultStoragePath: String
        get() {
            return try {
                val path = if (possibleStoragePaths.size > 1) {
                    //using sd card
                    possibleStoragePaths[StorageType.SD] ?: throw Exception("not path")
                } else {
                    //using internal storage
                    possibleStoragePaths[StorageType.PHONE] ?: throw Exception("not path")
                }
                val pathFile = File(path)
                if (!pathFile.mkdirs() && !pathFile.exists()) {
                    throw Exception("no folder exist")
                }
                return path
            } catch (ex: Exception) {
                // if something error happen, then using default internal storage
                // no need permission
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath + "/"
            }
        }

    val storageType: StorageType
        get() {
            possibleStoragePaths.forEach {
                if (it.value == defaultStoragePath) {
                    return it.key
                }
            }
            // Should not be hit, but default phone
            return StorageType.PHONE
        }

    const val calibration = "calibration"
    private const val data = "data"

    /**
     * Get specific full path of the folder
     */
    fun getSpecificStorageFolderPath(dateStr: String): String {
        return "$defaultStoragePath$data/$dateStr/${AppUtils.deviceID}/${AppUtils.IMEI}/"
    }

    /**
     * Get specific full path of the folder
     */
    fun getSpecificCalibrationFolderPath(dateStr: String): String {
        return "$defaultStoragePath$calibration/$dateStr/${AppUtils.deviceID}/${AppUtils.IMEI}/"
    }

    fun listDataSessions(): List<String> {
        val dataPath = File("$defaultStoragePath$data")
        return dataPath.listFiles()?.map { it.name } ?: emptyList()
    }
}