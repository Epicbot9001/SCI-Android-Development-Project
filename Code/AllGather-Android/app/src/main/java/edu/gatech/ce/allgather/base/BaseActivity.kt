package edu.gatech.ce.allgather.base

import androidx.appcompat.app.AppCompatActivity
import pub.devrel.easypermissions.EasyPermissions

/**
 * Base Activity
 * @author Justin Lee
 * @date 2020/6/15
 */
abstract class BaseActivity : AppCompatActivity() {
    val TAG = this::class.java.simpleName

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}