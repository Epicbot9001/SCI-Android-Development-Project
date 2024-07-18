package edu.gatech.ce.allgather.old.utils

import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import edu.gatech.ce.allgather.AllGatherApplication
import edu.gatech.ce.allgather.old.helpers.CameraHelper
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.io.File

/**
 * Created by andyleekung on 1/19/18.
 */
object StopVehicleFilter {
    private val TAG = "StopVehicleFilter"

    val movingPoints: ArrayList<Location> = ArrayList()

    val stoppedPoints: ArrayList<Location> = ArrayList()

    val stoppedVideoFilesSubject: PublishSubject<File> = PublishSubject.create()

    val movingLocationFilterSubject: PublishSubject<Location> = PublishSubject.create()

    val isMovingSubject: PublishSubject<Boolean> = PublishSubject.create()

    private var lastVideoFile: File? = null
    private var newVideoFile = true
    private var stoppedVideoSubscription: Disposable? = null

    private var previousLocation: Location? = null
    private var previousTimestamp: Long? = null

    private val pointsBuffer: ArrayList<Location> = ArrayList()
    private var distance: Float = 0F
    private var time: Float = 0F

    private val distanceResults = FloatArray(3)

    private val MIN_SPEED = 5.0.mphToMetersPerMS() // 0.00134112 m/ms = 3 mph

    private val BUFFER_SIZE = AllGatherApplication.MINI_VIDEOS_LENGTH_SECONDS

    private fun Double.mphToMetersPerMS(): Double = this * 0.00044704

    private val stoppedLocationBuffer: ArrayList<Location> = ArrayList()

    fun startFilter() {
        Log.d(TAG, "Started Vehicle Filter")
        stoppedVideoSubscription = CameraHelper.videoFilesSubject.subscribe { file ->
            if (lastVideoFile != null && lastVideoFile!!.absolutePath == file.absolutePath) {
                newVideoFile = false
            } else {
                lastVideoFile = file
                newVideoFile = true
            }
        }
    }

    fun stopFilter() {
        Log.d(TAG, "Stopped GPS Filter")
        if (stoppedVideoSubscription != null && !stoppedVideoSubscription!!.isDisposed) {
            stoppedVideoSubscription?.dispose()
        }
    }

    fun addPoint(location: Location) {
        val latlng = LatLng(location.latitude, location.longitude)
        if (previousLocation == null) {
            movingPoints.add(location)
            previousLocation = location
            previousTimestamp = location.time
            return
        }
        val dT = location.time - previousTimestamp!!
        android.location.Location.distanceBetween(
            previousLocation!!.latitude,
                previousLocation!!.longitude, latlng.latitude, latlng.longitude, distanceResults
        )
        val distanceBetween = distanceResults[0]

        // for video file deleting
        if (pointsBuffer.size > BUFFER_SIZE) {
            val speed = distance / time
            Log.d(TAG, "Speed: $speed")
            if (speed > MIN_SPEED) {
                movingPoints.addAll(pointsBuffer)
            } else {
                stoppedPoints.addAll(pointsBuffer)
                if (lastVideoFile != null && newVideoFile) {
                    Log.d(TAG, "Stopped Video: ${lastVideoFile?.absolutePath}")
                    stoppedVideoFilesSubject.onNext(lastVideoFile!!)
                }
            }

            distance = 0F
            time = 0F
            pointsBuffer.clear()
        }

        /**
         * The GPS filtering works by calculating each speed between the current point and the
         * "previous location". If the speed is less than the minimum accepted speed then it it
         * is added to a stopped point buffer. Once a speed has been calculated to be over the
         * accepted speed, the first and last point in the stopped point buffer will be recorded
         * and the rest will be removed. If the buffer has less than 3 points, all the points
         * will be recorded.
         */
        if (AllGatherApplication.USE_GPS_FILTER) {
            // for stopped GPS location filtering
            if (distanceBetween / dT > MIN_SPEED) {
                // if there are points in the stopped location buffer and we are moving, push the
                // first and last stopped points
                if (stoppedLocationBuffer.isNotEmpty()) {
                    if (stoppedLocationBuffer.size >= 2) {
                        movingLocationFilterSubject.onNext(stoppedLocationBuffer.first())
                        movingLocationFilterSubject.onNext(stoppedLocationBuffer.last())
                    } else {
                        // push the only point
                        movingLocationFilterSubject.onNext(stoppedLocationBuffer.first())
                    }
                    stoppedLocationBuffer.clear()
                }
                // push the most recent location if there is movement
                movingLocationFilterSubject.onNext(location)

                isMovingSubject.onNext(true)

            } else {
                // if it is not moving add to the stopped location buffer
                stoppedLocationBuffer.add(location)
                // if there is more than 5 points that are stopped, then tell subscribers we are not
                // moving
                if (stoppedLocationBuffer.size >= 5) {
                    isMovingSubject.onNext(false)
                }
            }
        } else {
            movingLocationFilterSubject.onNext(location)
        }


        pointsBuffer.add(location)
        distance += distanceBetween
        time += dT

        // only update the previous location if the average speed is greater than MIN_SPEED
        if (distance / time > MIN_SPEED) {
            previousLocation = location
        }

        previousTimestamp = location.time
    }
}