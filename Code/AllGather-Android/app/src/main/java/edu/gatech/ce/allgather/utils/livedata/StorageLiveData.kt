package edu.gatech.ce.allgather.utils.livedata

import android.os.Handler
import android.os.HandlerThread
import android.os.StatFs
import androidx.lifecycle.LiveData
import edu.gatech.ce.allgather.helpers.StorageHelper
import kotlin.properties.Delegates

/**
 * Storage Live Data
 * @author Justin Lee
 * @date 2020/6/11
 */
class StorageLiveData : LiveData<StorageLiveData.StorageBean>() {
    /**
     * This data bean is using for storage Storage's Information
     *
     * @param totalSpace
     * @param availableSpace
     * Created by Justin Lee
     */
    data class StorageBean(val totalSpace: Long, val availableSpace: Long)

    companion object {
        private const val STORAGE_UPDATE_FREQUENCY = 15_000
    }

    /** [HandlerThread] where storage capacity self-check run */
    private var storageThread: HandlerThread? = null

    /** [Handler] corresponding to [storageThread] */
    private var storageHandler: Handler? = null
    var totalSpace by Delegates.notNull<Long>()

    init {
        //todo need check permission
        val statFs = StatFs(StorageHelper.defaultStoragePath)
        totalSpace = statFs.blockCountLong * statFs.blockSizeLong
        val availableSpace = statFs.blockSizeLong * statFs.availableBlocksLong
        postValue(StorageBean(totalSpace, availableSpace))
    }

    override fun onActive() {
        super.onActive()
        storageThread = HandlerThread("StorageThread").apply {
            start()
        }
        storageHandler = Handler(storageThread!!.looper)
        storageHandler?.post { getCurrentStorageInfo() }
    }

    override fun onInactive() {
        super.onInactive()
        storageHandler?.removeCallbacksAndMessages(null)
        storageThread?.quitSafely()
        storageHandler = null
        storageHandler = null
    }

    private fun getCurrentStorageInfo() {
        val statFs = StatFs(StorageHelper.defaultStoragePath)
        val availableSpace = statFs.blockSizeLong * statFs.availableBlocksLong
        postValue(StorageBean(totalSpace, availableSpace))
        storageHandler?.apply {
            postDelayed({ getCurrentStorageInfo() }, STORAGE_UPDATE_FREQUENCY.toLong())
        }
    }
}