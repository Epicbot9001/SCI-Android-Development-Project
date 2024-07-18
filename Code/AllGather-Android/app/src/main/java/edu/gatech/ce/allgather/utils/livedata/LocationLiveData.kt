package edu.gatech.ce.allgather.utils.livedata

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import edu.gatech.ce.allgather.AllGatherApplication
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * LocationLiveData
 * @author Justin Lee
 * @date 2020/6/10
 */
class LocationLiveData(val context: Context) : LiveData<LocationLiveData.LocationInfoBean>() {
    companion object {
        //update frequency (ms)
        const val UPDATE_FREQUENCY = 100L
        //update distance (m)
        const val UPDATE_DISTANCE = 0.1f
    }

    private val _locationUpdatesFlow = MutableSharedFlow<LocationInfoBean>(extraBufferCapacity = 100)
    val locationUpdatesFlow = _locationUpdatesFlow.asSharedFlow()

    private lateinit var locationManager: LocationManager
    internal var latestLocation: Location? = null

    // Flag to determine when to start publishing location updates
    var shouldPublishLocationUpdates = false

    init {
        // Log the process id
        if (context is Activity) {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }

    var isReady: Boolean = false
        get() = latestLocation != null

    private var listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val locationInfoBean = LocationInfoBean(location, true)
            if (shouldPublishLocationUpdates) {
                _locationUpdatesFlow.tryEmit(locationInfoBean) // Flow updates
            }
            postValue(locationInfoBean)
            latestLocation = location
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {
            val locationInfoBean = LocationInfoBean(latestLocation, true)
            if (shouldPublishLocationUpdates) {
                _locationUpdatesFlow.tryEmit(locationInfoBean) // Flow updates
            }
            postValue(locationInfoBean)
        }

        override fun onProviderDisabled(provider: String) {
            val locationInfoBean = LocationInfoBean(latestLocation, false)
            if (shouldPublishLocationUpdates) {
                _locationUpdatesFlow.tryEmit(locationInfoBean) // Flow updates
            }
            postValue(locationInfoBean)
        }
    }

    override fun onActive() {
        super.onActive()
        //todo request location
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        //Todo: some device might not work, should request google serve?
        if (AllGatherApplication.MIN_DISTANCE_UPDATE)
        {
            /**
             * Distance based trigger
             * @author Tianqi Liu
             * @date 2020/7/21
             */
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, UPDATE_DISTANCE, listener)
        }
        else
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_FREQUENCY, 0f, listener)
        }
    }

    override fun onInactive() {
        super.onInactive()
        locationManager.removeUpdates(listener)
    }

    data class LocationInfoBean(val location: Location?, val isGpsWork: Boolean)
}