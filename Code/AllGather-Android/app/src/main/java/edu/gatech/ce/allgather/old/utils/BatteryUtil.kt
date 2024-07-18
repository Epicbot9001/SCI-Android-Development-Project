package edu.gatech.ce.allgather.old.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Created by achatterjee36 on 7/7/2017.
 */

@Deprecated(message = "old",replaceWith = ReplaceWith("BatteryLiveData"))
class BatteryUtil(internal var context: Context) {
    private var batteryStatus: Intent?

    init {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        batteryStatus = context.registerReceiver(null, ifilter)
    }

    fun getBatteryPercent(): Int {
        //get battery percentage
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 1
        return (100 * level / scale.toFloat()).toInt()
    }
}
