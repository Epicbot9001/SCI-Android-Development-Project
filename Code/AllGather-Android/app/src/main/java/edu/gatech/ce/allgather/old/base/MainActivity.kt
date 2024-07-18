package edu.gatech.ce.allgather.old.base

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler

import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar

import com.google.android.material.navigation.NavigationView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import android.telephony.TelephonyManager

import android.util.Log

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import edu.gatech.ce.allgather.AllGatherApplication
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.ui.setting.SettingsActivity
import edu.gatech.ce.allgather.old.helpers.AccelerationHelper
import edu.gatech.ce.allgather.old.helpers.CameraHelper
import edu.gatech.ce.allgather.old.helpers.LocationHelper
import edu.gatech.ce.allgather.old.utils.AutoFitTextureView
import edu.gatech.ce.allgather.old.utils.BatteryUtil
import edu.gatech.ce.allgather.old.utils.StopVehicleFilter
import edu.gatech.ce.allgather.old.utils.StorageUtil
import io.reactivex.disposables.Disposable
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var isReadyToToggle = true
    private var isStopped = true
    private lateinit var accHelper: AccelerationHelper
    private lateinit var camHelper: CameraHelper
    private lateinit var locHelper: LocationHelper
    private lateinit var battUtil: BatteryUtil
    private lateinit var strUtil: StorageUtil
    var isDeletingThreadEnabled = false
    private var mStoppedVideoSubscription: Disposable? = null
    private val mFilesToDelete: ArrayList<File> = ArrayList()
    lateinit var IMEI: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //keep the device awake
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //set up toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { togglePlay() }
        val camera_view = findViewById<AutoFitTextureView>(R.id.camera_view)
        camera_view.setOnClickListener { togglePlay() }

        val drawer_layout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        val nav_view = findViewById<NavigationView>(R.id.nav_view)
        nav_view.setNavigationItemSelectedListener(this)

        //initialize progress bars
        val batteryProgressBar = findViewById<ProgressBar>(R.id.batteryProgressBar)
        val storageProgressBar = findViewById<ProgressBar>(R.id.storageProgressBar)

        batteryProgressBar.max = 100
        storageProgressBar.max = 100

        //show testing text
        val testingText = findViewById<TextView>(R.id.testingText)
        if(AllGatherApplication.SHOW_FOR_TESTING_PURPOSES_ONLY_TEXT)
            testingText.visibility = View.VISIBLE
        else
            testingText.visibility = View.INVISIBLE

        // check for the required permissions
        checkPermissions()

        //initialize helpers
        accHelper = AccelerationHelper(this)
        camHelper = CameraHelper(this, camera_view)
        locHelper = LocationHelper(this)
        battUtil = (application as AllGatherApplication).batteryUtil
        strUtil = (application as AllGatherApplication).storageUtil

        val telephonyManager = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            /* 11/19/2019 Tianqi Liu
                From Android Q, the IMEI is unavailable to non-system applications
                IMEI = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "IMEINotAvailable" else
                   if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) telephonyManager.imei else telephonyManager.getDeviceId()

             */
            IMEI = "IMEINotAvailable"
        }


        // use BatteryUtil and update the battery display
        updateBatteryDisplay()
        // use StorageUtil and update the storage display
        updateStorageDisplay()

        // get setting values as preferenceManager
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        // check for setting - display the cross if camera switched off
        val disable_sign = findViewById<ImageView>(R.id.disable_sign)
        if (sharedPref.getBoolean(this.resources.getString(R.string.pref_camera_switch),false))
        {
            disable_sign.visibility = View.VISIBLE
        } else {
            disable_sign.visibility = View.INVISIBLE
        }

    }

    private fun togglePlay() //the start/stop button was clicked
    {
        // get setting values as preferenceManager
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val fab = findViewById<FloatingActionButton>(R.id.fab)

        if (!isReadyToToggle)
        //if cannot start/stop right now, show a snackbar
        {
            Snackbar.make(fab, R.string.cannot_start_stop, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            return
        }
        val currTime = System.currentTimeMillis()
        // check if expiry date has passed
        if(AllGatherApplication.EXPIRY_DATE.before(Date(currTime)))
        {
            Toast.makeText(this, R.string.expiry_date_passed,
                    Toast.LENGTH_LONG).show()
            return
        }
        if(AllGatherApplication.EXPIRY_WARNING_DATE.before(Date(currTime)))
        {
            Toast.makeText(this, R.string.expiry_date_passed,
                    Toast.LENGTH_LONG).show()
        }
        isReadyToToggle = false
        if (isStopped)
        //was stopped. try to start recording
        {
            try {//I can see the protected methods here for some reason. I thought they were not supposed to be accessible from outside?

                // check for setting - do not record video if camera intended to be switched off
                if (!sharedPref.getBoolean(this.resources.getString(R.string.pref_camera_switch),false)) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                    isDeletingThreadEnabled = true
                    // set the camHelper's fileNumber so that saved files are reset at 0 and increment
                    camHelper.isFirstRecording = true
                    camHelper.startRecording(currTime)
                    accHelper.startRecording(currTime)
                    locHelper.startRecording(currTime)
                    writeLogStart(currTime)
                } else {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                    isDeletingThreadEnabled = true
                    // set the camHelper's fileNumber so that saved files are reset at 0 and increment
                    accHelper.startRecording(currTime)
                    locHelper.startRecording(currTime)
                    writeLogStart(currTime)
                }


                if (AllGatherApplication.USE_MINI_VIDEOS) {
                    StopVehicleFilter.startFilter()
                    mStoppedVideoSubscription = StopVehicleFilter.stoppedVideoFilesSubject.subscribe { deleteVideoFile ->
                        Log.d(TAG, "File marked for deletion ${deleteVideoFile.absolutePath}")
                        mFilesToDelete.add(deleteVideoFile)
                    }
                }


            }//TODO: make more descriptive
//            catch (ex: IllegalStateException) {
//                Snackbar.make(fab, R.string.cannot_start_stop, Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show()
//                ex.printStackTrace()
//                return
//
//            }
            catch (ex: Exception) {
                Snackbar.make(fab, R.string.cannot_start_stop, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                ex.printStackTrace()
                return
            }

            // no problems, recording started. Updated UI
            //change fab icon
            fab.setImageResource(R.drawable.ic_media_stop)
            //red outline
            val main_linear_layout = findViewById<LinearLayout>(R.id.main_linear_layout)
            main_linear_layout.setBackgroundResource(R.drawable.recording_border)
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent))
            //remove crosshairs
            val crosshair = findViewById<ImageView>(R.id.crosshair)
            val crosshair2 = findViewById<ImageView>(R.id.crosshair2)
            crosshair.visibility = View.INVISIBLE
            crosshair2.visibility = View.INVISIBLE


            // place ongoing notification
            val intent = Intent(this, MainActivity::class.java)
            val pIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val nb = Notification.Builder(applicationContext)
                    .setContentTitle(resources.getString(R.string.recording_in_progress))
                    .setContentIntent(pIntent)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setOngoing(true)
            val n = nb.build()
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(1, n)
            val fab = findViewById<FloatingActionButton>(R.id.fab)
            Snackbar.make(fab, R.string.started_recording, Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show()
            isStopped = false
        } else
        //was recording. stop and save
        {
            // check camera switch setting again
            if (!sharedPref.getBoolean(this.resources.getString(R.string.pref_camera_switch),false))
            {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                isDeletingThreadEnabled = false
                camHelper.stopRecording()
                accHelper.stopRecording()
                locHelper.stopRecording()
                writeLogEnd()
            } else {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                isDeletingThreadEnabled = false
                accHelper.stopRecording()
                locHelper.stopRecording()
                writeLogEnd()
            }

            if (AllGatherApplication.USE_MINI_VIDEOS) {
                StopVehicleFilter.stopFilter()
                mFilesToDelete.forEach {
                    it.delete()
                    Log.d(TAG, "${it.absolutePath} deleted")
                }
                mFilesToDelete.clear()
            }
            if (mStoppedVideoSubscription != null && !mStoppedVideoSubscription!!.isDisposed) {
                mStoppedVideoSubscription?.dispose()
            }

            val fab = findViewById<FloatingActionButton>(R.id.fab)
            val main_linear_layout = findViewById<LinearLayout>(R.id.main_linear_layout)
            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            val crosshair = findViewById<ImageView>(R.id.crosshair)
            val crosshair2 = findViewById<ImageView>(R.id.crosshair2)

            //stop recording
            //change fab icon
            fab.setImageResource(R.drawable.ic_media_play)
            //remove red outline
            main_linear_layout.setBackgroundColor(0x00ffffff)
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            crosshair.visibility = View.VISIBLE
            crosshair2.visibility = View.VISIBLE
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(1)
            Snackbar.make(fab, R.string.stopped_recording, Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show()
            isStopped = true
        }
        isReadyToToggle = true
    }

    override fun onBackPressed() {
        val drawer_layout = findViewById<DrawerLayout>(R.id.drawer_layout)

        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_settings) {
            // allow exit only if it is not recording
            if (isStopped) {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, R.string.stop_recording_for_settings, Toast.LENGTH_SHORT).show()
            }

            return true
        }


        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        when (id) {
//            R.id.nav_gallery -> {
//                // Settings
//                // allow exit only if it is not recording
//                if (isStopped) {
//                    val intent = Intent(this, GalleryActivity::class.java)
//                    intent.putExtra(getString(R.string.extras_storage_location),strUtil.getOverallAppStorageFolderPath())
//                    startActivity(intent)
//                } else {
//                    Toast.makeText(this, R.string.stop_recording_for_gallery, Toast.LENGTH_SHORT).show()
//                }
//            }
            R.id.nav_manage -> {
                // Settings
                // allow exit only if it is not recording
                if (isStopped) {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, R.string.stop_recording_for_settings, Toast.LENGTH_SHORT).show()
                }
            }
            else -> {

            }
        }

        val drawer_layout = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION)
                    , REQUEST_ALL_PERMISSIONS
            )

        }
    }

    fun updateLocationUI(loc : Location?) {
        val gpsSignalTextView = findViewById<TextView>(R.id.gpsSignalTextView)

        if (loc != null) {
            gpsSignalTextView.text = resources.getString(R.string.gps_signal_connected)
            Log.d("AC12","Updating location UI:"+loc.toString())
        } else {
            gpsSignalTextView.text = resources.getString(R.string.gps_signal_disconnected)
            Log.d("AC12","Updating location UI: disconnected")
        }
        doUpdateGlow(gpsSignalTextView)
    }

/*
    fun updateLocationUI() {
        tv_displacement.setTextColor(0xFF009900.toInt())
        tv_displacement.text = resources.getString(R.string.ok)
        doUpdateGlow(tv_displacement)
    }


    fun updateAccelerationUI(ax: Float, ay: Float, az: Float) {
        tv_acceleration.setTextColor(0xFF009900.toInt())
        tv_acceleration.text = resources.getString(R.string.ok)

        doUpdateGlow(tv_acceleration)
    }

    fun updateRotationUI() {
        tv_angulardisplacement.setTextColor(0xFF009900.toInt())
        tv_angulardisplacement.text = resources.getString(R.string.ok)
        doUpdateGlow(tv_angulardisplacement)
    }

    fun updateAngularVelocityUI() {
        tv_angularvelocity.setTextColor(0xFF009900.toInt())
        tv_angularvelocity.text = resources.getString(R.string.ok)
        doUpdateGlow(tv_angularvelocity)
    }
    */

    fun updateCalibrationUI(pitch: Float, roll: Float) {
        //tv_rx.text = String.format("%.1f",x)
        //tv_ry.text = String.format("%.1f",y)
        //tv_rz.text = String.format("%.1f",z)
        //move crosshairs
        val windowmanager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = windowmanager.defaultDisplay.rotation
        val crosshair = findViewById<ImageView>(R.id.crosshair)
        val crosshair2 = findViewById<ImageView>(R.id.crosshair2)

        if (rotation == 0) {
            //crosshair.visibility = View.INVISIBLE
            //crosshair2.visibility = View.INVISIBLE

            crosshair.visibility = View.VISIBLE
            crosshair2.visibility = View.VISIBLE
            val rollAngle = roll + Math.PI / 2
            //Log.d("AC1","roll: "+rollAngle)
            val alpha: Float = 0.5f
            //crosshair2.rotation = crosshair2.rotation + alpha*(rollAngle*25-crosshair2.rotation)
            crosshair2.translationY = crosshair2.translationY + alpha * (rollAngle.toFloat() * 100 - crosshair2.translationY)
        } else if (rotation == 1) {
            crosshair.visibility = View.VISIBLE
            crosshair2.visibility = View.VISIBLE
            val pitchAngle = pitch + Math.PI / 2
            val alpha: Float = 0.5f
            //crosshair2.rotation = crosshair2.rotation + alpha*(rollAngle*25-crosshair2.rotation)
            crosshair2.translationY = crosshair2.translationY + alpha * (-pitchAngle.toFloat() * 100 - crosshair2.translationY)
        } else if (rotation == 3) {
            crosshair.visibility = View.VISIBLE
            crosshair2.visibility = View.VISIBLE
            val pitchAngle = pitch - Math.PI / 2
            val alpha: Float = 0.5f
            //crosshair2.rotation = crosshair2.rotation + alpha*(rollAngle*25-crosshair2.rotation)
            crosshair2.translationY = crosshair2.translationY + alpha * (pitchAngle.toFloat() * 100 - crosshair2.translationY)
        }

    }

    private fun updateBatteryUI(p: Int) {
        val batteryTextView = findViewById<TextView>(R.id.batteryTextView)
        val batteryProgressBar = findViewById<ProgressBar>(R.id.batteryProgressBar)

        batteryTextView.text = p.toString() + "%"
        batteryProgressBar.progress = p
    }

    private fun updateStorageUI(freeBytes: Long, totalBytes: Long) {
        var value = freeBytes.toFloat() / (1024 * 1024)
        var unit = "MB"
        if (value > 1024) {
            value /= 1024f
            unit = "GB"
        }

        if (value > 1024) {
            value /= 1024f
            unit = "TB"
        }

        var totalValue = totalBytes.toFloat() / (1024 * 1024)
        var totalUnit = "MB"
        if (totalValue > 1024) {
            totalValue /= 1024f
            totalUnit = "GB"
        }

        if (totalValue > 1024) {
            totalValue /= 1024f
            totalUnit = "TB"
        }

        val storageTextView = findViewById<TextView>(R.id.storageTextView)
        storageTextView.text = String.format("%.1f %s free of %.0f %s", value, unit, totalValue, totalUnit)

        val storageProgressBar = findViewById<ProgressBar>(R.id.storageProgressBar)
        storageProgressBar.progress = (freeBytes * 100 / totalBytes).toInt()
    }

    private fun doUpdateGlow(v: View) {
        val color2 = 0x00FFFF00
        val color1 = 0xFFFFFF00.toInt()
        val colorAnim = ObjectAnimator.ofInt(v, "backgroundColor", color1, color2)
        colorAnim.duration = 500
        colorAnim.setEvaluator(ArgbEvaluator())
        colorAnim.repeatCount = 0
        colorAnim.repeatMode = ValueAnimator.RESTART
        colorAnim.start()
    }

    private fun updateBatteryDisplay() {
        updateBatteryUI(battUtil.getBatteryPercent())
        val handler = Handler()
        handler.postDelayed({ updateBatteryDisplay() }, BATTERY_UPDATE_FREQUENCY.toLong())
    }

    private fun updateStorageDisplay() {
        // update the storage type
        val tv_storage = findViewById<TextView>(R.id.tv_storage)
        tv_storage.text = resources.getString(R.string.storage, strUtil.getStorageType().name)

        updateStorageUI(strUtil.updateStorageSpace(), strUtil.totalSpace)
        val handler = Handler()
        handler.postDelayed({ updateStorageDisplay() }, STORAGE_UPDATE_FREQUENCY.toLong())

    }



    override fun onResume() {
        super.onResume()
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        camHelper.onResume()

        // get setting values as preferenceManager
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        // check for setting - display the cross if camera switched off
        val disable_sign = findViewById<ImageView>(R.id.disable_sign)
        if (sharedPref.getBoolean(this.resources.getString(R.string.pref_camera_switch),false))
        {
            disable_sign.visibility = View.VISIBLE
        } else {
            disable_sign.visibility = View.INVISIBLE
        }

    }

    override fun onPause() {
        super.onPause()
        //if current mediaRecorded state was recording, then need to stop before calling camHelp.onPause
        if (!isStopped){
            togglePlay()
        }
        camHelper.onPause()
    }

    private fun writeLogStart(timestamp: Long) {
        //build string to write
        //append timestamp
        val dt = Date(timestamp)
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS")
        val sb = StringBuilder()
        sb.append("[")
        sb.append(sdf.format(dt))
        sb.append("] { message=\"Started recording\", ")
        //append driver id
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val driverid = sharedPref.getString(getString(R.string.pref_driverid), "-1")
        sb.append("driverId=\"$driverid\", ")
        sb.append("IMEI=\"$IMEI\", ")
        //append data version
        sb.append("dataVersion=\"${getString(R.string.data_version)}\"")
        sb.append("}\n")
        //write
        val folder = File(strUtil.getOverallAppStorageFolderPath())
        if (!folder.mkdirs()) {
            Log.d("AC1", "Main Directory not created")
        }
        try {
            val fos = FileOutputStream(File(folder, "log_$IMEI.txt"), true)
            fos.write(sb.toString().toByteArray())
            fos.close()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun logException(t: Throwable) {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        t.printStackTrace(printWriter)
        val stackTraceString = stringWriter.toString()
        val timestamp = System.currentTimeMillis()
        val date = Date(timestamp)

        //write
        val folder = File(strUtil.getOverallAppStorageFolderPath())
        if (!folder.mkdirs()) {
            Log.d("AC1", "Directory not created")
        }
        try {
            val fos = FileOutputStream(File(folder, "log_$IMEI.txt"), true)
            fos.write("Exception thrown at $date".toByteArray())
            fos.write(stackTraceString.toByteArray())
            fos.close()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun writeLogEnd() {
        //build string to write
        //append timestamp
        val timestamp = System.currentTimeMillis()
        val dt = Date(timestamp)
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS")
        val sb = StringBuilder()
        sb.append("[")
        sb.append(sdf.format(dt))
        sb.append("] { message=\"Ended recording\" }\n\n")
        //write
        val folder = File(strUtil.getOverallAppStorageFolderPath())
        if (!folder.mkdirs()) {
            Log.d("AC1", "Directory not created")
        }
        try {
            val fos = FileOutputStream(File(folder, "log_$IMEI.txt"), true)
            fos.write(sb.toString().toByteArray())
            fos.close()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    companion object {
        private val TAG = "MainActivity"
        val REQUEST_ALL_PERMISSIONS = 199
        val REQUEST_CAMERA_PERMISSION = 200
        val REQUEST_LOCATION_PERMISSION = 201
        val STORAGE_UPDATE_FREQUENCY = 15000
        val BATTERY_UPDATE_FREQUENCY = 60000
    }
}
