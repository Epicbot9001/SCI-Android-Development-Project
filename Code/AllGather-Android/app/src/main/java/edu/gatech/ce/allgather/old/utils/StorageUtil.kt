package edu.gatech.ce.allgather.old.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import androidx.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import android.telephony.TelephonyManager
import android.util.Log
import edu.gatech.ce.allgather.Processor
import edu.gatech.ce.allgather.R
import java.io.File

/**
 * Created by andyleekung on 9/6/17.
 */

@Deprecated(message = "old",replaceWith = ReplaceWith("StorageLiveData"))
class StorageUtil(val context: Context) {

    enum class StorageType(val type: String) {
        PHONE("Internal Storage"),
        SD("SD Card")
    }

    private var statFs: StatFs

    var totalSpace: Long
    var freeSpace: Long

    init {
        Log.d("AC1", Environment.getExternalStorageDirectory().absolutePath)
        statFs = StatFs(getOverallAppStorageFolderPath())
        totalSpace = statFs.blockCountLong * statFs.blockSizeLong
        freeSpace = updateStorageSpace()
    }

    fun updateStorageSpace(): Long {
        statFs = StatFs(getOverallAppStorageFolderPath())
        totalSpace = statFs.blockCountLong * statFs.blockSizeLong
        freeSpace = statFs.availableBlocksLong * statFs.blockSizeLong
        return freeSpace
    }

    public fun getOverallAppStorageFolderPath(): String {
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        val defaultPath = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.absolutePath + "/"
        val preferredPath = preferenceManager.getString(context.resources.getString(R.string.pref_storage_list), defaultPath)
        val preferredPathFile = File(preferredPath)
        // need to check if the path is still valid
        if (!preferredPathFile.mkdirs() && !preferredPathFile.exists()) {
            // if it is not valid, update preference
            val preferenceEditor = preferenceManager.edit()
            preferenceEditor.putString(context.resources.getString(R.string.pref_storage_list), defaultPath)
            preferenceEditor.apply()
            return defaultPath
        }

        return preferredPath!!
    }

    public fun getSpecificStorageFolderPath(datestring: String): String {

        val overallPath = getOverallAppStorageFolderPath()
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var imei = "0"
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            /* 11/19/2019 Tianqi Liu
                From Android Q, the IMEI is unavailable to non-system applications
                imei = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "Not Available" else
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) telephonyManager.imei else telephonyManager.getDeviceId()
             */
            imei = "IMEINotAvailable"

        }
        val specificFolderPath =  overallPath +
                    datestring + "/" +
                    sharedPref.getString(context.getString(R.string.pref_driverid), "0") + "/" +
                    imei + "/"
        Log.d("AC1",specificFolderPath)
        return specificFolderPath
    }

    // File path -> Type
    fun getPossibleStoragePaths(): List<Pair<String, StorageType>> {
        val storages = context.getExternalFilesDirs(Environment.DIRECTORY_DOCUMENTS)
                .asList()
        val storageListWithType = ArrayList<Pair<String, StorageType>>()
        // The first index is the internal storage, second is the SD card
        storages.forEachIndexed { index, file ->
            if (index == 0)
            {
                if(file != null)
                    storageListWithType.add(Pair("${file.absolutePath}/", StorageType.PHONE))
            }
            else
            {
                if(file != null)
                    storageListWithType.add(Pair("${file.absolutePath}/", StorageType.SD))
            }
        }
        return storageListWithType
    }

    fun getStorageType(): StorageType {
        val possiblePaths = getPossibleStoragePaths()
        val storagePath = getOverallAppStorageFolderPath()
        possiblePaths.forEach {
            if (it.component1() == storagePath) return it.component2()
        }
        // Should not be hit, but default phone
        return StorageType.PHONE
    }

    fun setStoragePrefDefault() {
        // set default SD
        val possibleStorages = getPossibleStoragePaths()
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        val preferenceEditor = preferenceManager.edit()
        if (possibleStorages.size > 1) {
            val sdStorage = possibleStorages[1]
            // second element should be SD
            preferenceEditor.putString(context.resources.getString(R.string.pref_storage_list), sdStorage.first)
            preferenceEditor.apply()

        } else {
            val internalStorage = possibleStorages[0]
            // first element should be internal storage
            preferenceEditor.putString(context.resources.getString(R.string.pref_storage_list), internalStorage.first)
        }
    }

    fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED
    }

    fun listSessionFolders(): Array<out File>? {
        val path = getOverallAppStorageFolderPath() + "data/"
        val directory = File(path)
        return directory.listFiles()
    }

    companion object {
        /* Checks if external storage is available for read and write */
        fun isExternalStorageWritable(): Boolean {
            val state = Environment.getExternalStorageState()
            return state == Environment.MEDIA_MOUNTED
        }

        /* Checks if external storage is available to at least read */
        fun isExternalStorageReadable(): Boolean {
            val state = Environment.getExternalStorageState()
            return state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
        }
    }


}