package edu.gatech.ce.allgather.utils.livedata

import android.app.Activity
import android.content.Context
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.LiveData

/**
 * Battery Live Data
 * @author Justin Lee
 * @date 2020/6/11
 */
class BatteryLiveData(val context: Context) : LiveData<Int>() {
    companion object {
        private const val BATTERY_UPDATE_FREQUENCY = 60_000
    }

    /** [HandlerThread] where battery self-check run */
    private var batteryThread: HandlerThread? = null

    /** [Handler] corresponding to [batteryThread] */
    private var batteryHandler: Handler? = null

    private lateinit var batteryManager: BatteryManager

    init {
        if (context is Activity) {
            batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        }
    }

    override fun onActive() {
        super.onActive()
        batteryThread = HandlerThread("BatteryThread").apply {
            start()
        }
        batteryHandler = Handler(batteryThread!!.looper)
        batteryHandler?.post { getBatteryInformation() }
    }

    override fun onInactive() {
        super.onInactive()
        batteryHandler?.removeCallbacksAndMessages(null)
        batteryThread?.quitSafely()
        batteryHandler = null
        batteryThread = null
    }

    /**
     * get battery information
     *
     * Created by Justin Lee
     */
    private fun getBatteryInformation() {
        val currentBatteryCapacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        postValue(currentBatteryCapacity)
        batteryHandler?.apply {
            postDelayed({ getBatteryInformation() }, BATTERY_UPDATE_FREQUENCY.toLong())
        }
    }

}