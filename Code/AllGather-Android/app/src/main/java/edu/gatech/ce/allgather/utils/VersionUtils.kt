package edu.gatech.ce.allgather.utils

import android.content.Context
import android.content.pm.PackageManager


/**
 * version utils
 * @author Justin Lee
 * @date 2020/6/18
 */
object VersionUtils {
    /**
     * Get version code
     *
     * @param context
     * Created by Justin Lee
     */
    fun getVersionCode(context: Context): Int {
        var versionCode = 0
        try {
            versionCode = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return versionCode
    }

    /**
     * Get version name
     *
     * @param context
     * Created by Justin Lee
     */
    fun getVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            ""
        }
    }
}