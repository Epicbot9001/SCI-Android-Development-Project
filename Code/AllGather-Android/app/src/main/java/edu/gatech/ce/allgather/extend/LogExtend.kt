package edu.gatech.ce.allgather.extend

import android.util.Log
import edu.gatech.ce.allgather.base.BaseActivity
import edu.gatech.ce.allgather.base.BaseFragment

/**
 * Log extend
 * @author Justin Lee
 * @date 2020/6/8
 */

const val LOG_TAG = "ALL_GATHER"

fun BaseActivity.log(message: String?) {
    logMessage("Class:$TAG - Message:$message")
}

fun BaseFragment.log(message: String?){
    logMessage("Class:$TAG - Message:$message")
}

fun logMessage(message: String) {
    Log.d(LOG_TAG, message)
}