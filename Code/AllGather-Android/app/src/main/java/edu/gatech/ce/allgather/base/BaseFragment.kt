package edu.gatech.ce.allgather.base

import androidx.fragment.app.Fragment
import pub.devrel.easypermissions.EasyPermissions

/**
 * Base Fragment
 * @author Justin Lee
 * @date 2020/6/15
 */
abstract class BaseFragment : Fragment() {
    val TAG = this::class.java.simpleName

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}