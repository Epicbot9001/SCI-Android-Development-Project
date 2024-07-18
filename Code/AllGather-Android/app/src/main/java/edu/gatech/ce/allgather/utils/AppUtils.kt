package edu.gatech.ce.allgather.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.preference.PreferenceManager
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import edu.gatech.ce.allgather.AllGatherApplication
import edu.gatech.ce.allgather.R

/**
 * AppUtils
 * @author Justin Lee
 * @date 2020/6/15
 */
object AppUtils {
    val appContext = AllGatherApplication.instance.applicationContext

    val IMEI: String
        get() {
            val telephonyManager = appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (ActivityCompat.checkSelfPermission(appContext,
                            Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                /* 11/19/2019 Tianqi Liu
                    From Android Q, the IMEI is unavailable to non-system applications
                                    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "IMEINotAvailable" else
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) telephonyManager.imei else telephonyManager.getDeviceId()
                 */
                return "IMEINotAvailable"

            }
            return "0"
        }

    val deviceID: String
        get() {
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext)
            return sharedPref.getString(appContext.getString(R.string.pref_driverid), "0") ?: "0"
        }
}
