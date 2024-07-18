package edu.gatech.ce.allgather.utils

import edu.gatech.ce.allgather.BuildConfig
import edu.gatech.ce.allgather.helpers.RecordLogHelper

/**
 * Record interface
 * @author Justin Lee
 * @date 2020/6/15
 */
interface IRecord
/**
 * recording unexpected error
 *
 * @param exception
 * Created by Justin Lee
 */
fun IRecord.recordLog(exception: Exception) {
    if (BuildConfig.DEBUG) {
        RecordLogHelper.log(exception)
    }
}