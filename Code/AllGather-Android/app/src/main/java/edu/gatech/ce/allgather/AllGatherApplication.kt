package edu.gatech.ce.allgather

import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import edu.gatech.ce.allgather.old.utils.BatteryUtil
import edu.gatech.ce.allgather.old.utils.StorageUtil
import java.util.Date

class AllGatherApplication : MultiDexApplication() {
    companion object {
        // toggle if you want to allow mini videos
        // also enables stopped vehicle filtering
        // maybe change?
        const val USE_MINI_VIDEOS = false
        const val USE_GPS_FILTER = false
        const val STOPPED_VEHICLE_FILTER = false
        const val MINI_VIDEOS_LENGTH_SECONDS = 10
        const val MIN_DISTANCE_UPDATE = false
        // toggle for acceleration and location data encryption
        const val ENCRYPT_DATA = false

        // expiry date/time
        private const val expiry_utc = 4132270800000 //year 2100. Basically no expiry date
        //private const val expiry_utc = 1609372800000
        val EXPIRY_DATE = Date(expiry_utc)
        val EXPIRY_WARNING_DATE = Date(expiry_utc - (2 * 7 * 24 * 60 * 60 * 1000))

        // show text "For Testing Purposes Only"
        const val SHOW_FOR_TESTING_PURPOSES_ONLY_TEXT = false

        lateinit var instance: AllGatherApplication

    }

    lateinit var storageUtil: StorageUtil
    lateinit var batteryUtil: BatteryUtil

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        // initialize singleton objects
        storageUtil = StorageUtil(this)
        batteryUtil = BatteryUtil(this)

    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

}