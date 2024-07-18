package edu.gatech.ce.allgather.ui.calibration

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.extend.setOnClickDebounceListener
import edu.gatech.ce.allgather.helpers.CalibrationHelper
import edu.gatech.ce.allgather.ui.camera.CameraActivity
import edu.gatech.ce.allgather.utils.livedata.SensorInfoLiveData
import kotlin.math.abs

class CalibrationActivity : AppCompatActivity() {

    private lateinit var mParkTextView: TextView
    private lateinit var mParkOKButton: Button
    private lateinit var mParkDoneTextView: TextView

    private lateinit var mStat1TextView: TextView
    private lateinit var mStart1Button: Button
    private lateinit var mStat1ProgressBar: ProgressBar
    private lateinit var mStatDone1TextView: TextView

    private lateinit var firstBBITextView: TextView

    private lateinit var mTurnTextView: TextView
    private lateinit var mTurnOKButton: Button
    private lateinit var mTurnDoneTextView: TextView

    private lateinit var mStat2TextView: TextView
    private lateinit var mStart2Button: Button
    private lateinit var mStat2ProgressBar: ProgressBar
    private lateinit var mStatDone2TextView: TextView

    private lateinit var secondBBITextView: TextView

    private lateinit var mTurn3TextView: TextView
    private lateinit var mTurn3OKButton: Button
    private lateinit var mTurn3DoneTextView: TextView

    private lateinit var mStat3TextView: TextView
    private lateinit var mStart3Button: Button
    private lateinit var mStat3ProgressBar: ProgressBar
    private lateinit var mStatDone3TextView: TextView

    private lateinit var thirdBBITextView: TextView
    private lateinit var calibrationSuccessOrFailTextView: TextView

    private lateinit var mExitButton: Button
    private lateinit var mRestartButton: Button

    private lateinit var recordHelper: CalibrationHelper
    private lateinit var sensorInfoLiveData: SensorInfoLiveData

    companion object {
        private const val CALIBRATION_THRESHOLD = 0.1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        //requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        mParkTextView = findViewById(R.id.cal_tv_park)
        mParkOKButton = findViewById(R.id.cal_but_parkOK)
        mParkDoneTextView = findViewById(R.id.cal_tv_parkDone)

        mStat1TextView = findViewById(R.id.cal_tv_stat1)
        mStart1Button = findViewById(R.id.cal_but_start1)
        mStat1ProgressBar = findViewById(R.id.cal_pb_1)
        mStatDone1TextView = findViewById(R.id.cal_tv_done1)

        firstBBITextView = findViewById(R.id.firstBBIText)

        mTurnTextView = findViewById(R.id.cal_tv_turn)
        mTurnOKButton = findViewById(R.id.cal_but_turnOK)
        mTurnDoneTextView = findViewById(R.id.cal_tv_turnDone)

        mStat2TextView = findViewById(R.id.cal_tv_stat2)
        mStart2Button = findViewById(R.id.cal_but_start2)
        mStat2ProgressBar = findViewById(R.id.cal_pb_2)
        mStatDone2TextView = findViewById(R.id.cal_tv_done2)

        secondBBITextView = findViewById(R.id.secondBBIText)

        mTurn3TextView = findViewById(R.id.cal_tv_turn2)
        mTurn3OKButton = findViewById(R.id.cal_but_turnOK2)
        mTurn3DoneTextView = findViewById(R.id.cal_tv_turnDone2)

        mStat3TextView = findViewById(R.id.cal_tv_stat3)
        mStart3Button = findViewById(R.id.cal_but_start3)
        mStat3ProgressBar = findViewById(R.id.cal_pb_3)
        mStatDone3TextView = findViewById(R.id.cal_tv_done3)

        thirdBBITextView = findViewById(R.id.thirdBBIText)
        calibrationSuccessOrFailTextView = findViewById(R.id.calibrationSuccessOrFail)

        mExitButton = findViewById(R.id.cal_but_exit)
        mRestartButton = findViewById(R.id.cal_but_restart)

        recordHelper = CalibrationHelper(this)
        sensorInfoLiveData = SensorInfoLiveData(this)

        initComponents()

        //actual calibration buttons initially inivisible
        mStart1Button.visibility = View.GONE;
        mStat1TextView.visibility = View.GONE;

        mStart2Button.visibility = View.GONE;
        mStat2TextView.visibility = View.GONE;

        mStart3Button.visibility = View.GONE;
        mStat3TextView.visibility = View.GONE;


        mExitButton.setOnClickDebounceListener {
            val camIntent = Intent(this, CameraActivity::class.java)
            startActivity(camIntent)
            finish()

        }

        mRestartButton.setOnClickDebounceListener {
            finish();
            startActivity(intent)
        }

        mParkOKButton.setOnClickDebounceListener {
            mParkOKButton.isEnabled = false
            mParkOKButton.isVisible = false
            mStart1Button.isEnabled = true

            //Make calibration 1 buttons appear and mparkOK text disappear
            mParkTextView.visibility = View.GONE;
            mStart1Button.visibility = View.VISIBLE;
            mStat1TextView.visibility = View.VISIBLE;
        }

        mStart1Button.setOnClickDebounceListener {
            mStart1Button.isEnabled = false
            mStart1Button.isVisible = false
            mStat1ProgressBar.isVisible = true
            Handler(Looper.getMainLooper()).postDelayed({
                // start recording after 5s
                recordHelper.startRecord(0)
                recording()

            }, 5000/* 5 second */)

            Handler(Looper.getMainLooper()).postDelayed({
                // stop recording after 15s (5s + 10s)
                recordHelper.stopRecord()
                mStat1ProgressBar.isVisible = false
                mStatDone1TextView.isVisible = true
                mTurnOKButton.isEnabled = true
            }, 15000/* 10 second */)

        }

        mTurnOKButton.setOnClickDebounceListener {
            mTurnOKButton.isEnabled = false
            mTurnOKButton.isVisible = false
            mStart2Button.isEnabled = true

            //Make calibration 2 visible
            mTurnTextView.visibility = View.GONE;
            mStart2Button.visibility = View.VISIBLE;
            mStat2TextView.visibility = View.VISIBLE;
        }

        mStart2Button.setOnClickDebounceListener {
            mStart2Button.isEnabled = false
            mStart2Button.isVisible = false
            mStat2ProgressBar.isVisible = true
            Handler(Looper.getMainLooper()).postDelayed({
                // start recording after 5s
                recordHelper.startRecord(1)
                recording()

            }, 5000/* 5 second */)

            Handler(Looper.getMainLooper()).postDelayed({
                // stop recording after 15s (5s + 10s)
                recordHelper.stopRecord()
                mStat2ProgressBar.isVisible = false
                mStatDone2TextView.isVisible = true
                mTurn3OKButton.isEnabled = true

                val transformedBBI = recordHelper.getTransformedBBI(listOf(0, 1))
                firstBBITextView.text = transformedBBI[0].toString()
                firstBBITextView.isVisible = true
                secondBBITextView.text = transformedBBI[1].toString()
                secondBBITextView.isVisible = true
            }, 15000/* 10 second */)
        }

        mTurn3OKButton.setOnClickDebounceListener {
            mTurn3OKButton.isEnabled = false
            mTurn3OKButton.isVisible = false
            mStart3Button.isEnabled = true

            //Make calibration 3 visible
            mTurn3TextView.visibility = View.GONE;
            mStart3Button.visibility = View.VISIBLE;
            mStat3TextView.visibility = View.VISIBLE;
        }

        mStart3Button.setOnClickDebounceListener {
            mStart3Button.isEnabled = false
            mStart3Button.isVisible = false
            mStat3ProgressBar.isVisible = true
            Handler(Looper.getMainLooper()).postDelayed({
                // start recording after 5s
                recordHelper.startRecord(2)
                recording()
            }, 5000/* 5 second */)

            Handler(Looper.getMainLooper()).postDelayed({
                // stop recording after 15s (5s + 10s)
                recordHelper.stopRecord()
                mStat3ProgressBar.isVisible = false
                mStatDone3TextView.isVisible = true

                val transformedBBI = recordHelper.getTransformedBBI(listOf(0, 1, 2))
                thirdBBITextView.text = transformedBBI[2].toString()
                thirdBBITextView.isVisible = true

                calibrationSuccessOrFailTextView.isVisible = true
                if (abs(transformedBBI[2] - transformedBBI[0]) <= CALIBRATION_THRESHOLD) {
                    calibrationSuccessOrFailTextView.setTextColor(Color.parseColor("green"))
                    calibrationSuccessOrFailTextView.text = "Success"
                } else {
                    calibrationSuccessOrFailTextView.setTextColor(Color.parseColor("yellow"))
                    calibrationSuccessOrFailTextView.text = "Warning: Same Place BBI differs by more than ${CALIBRATION_THRESHOLD} degrees!"
                }

                recordHelper.startEndCalibration()
                recordHelper.endCalibraton()
            }, 15000/* 10 second */)
        }
    }

    override fun onBackPressed() {}


    private fun recording() {
        recordHelper.record(sensorInfoLiveData.value)
        Handler(Looper.getMainLooper()).postDelayed({ recording() }, CalibrationHelper.RECORDING_INTERVAL.toLong())
    }


    private fun initComponents() {
        mParkOKButton.isEnabled = true
        mParkOKButton.isVisible = true
        mParkDoneTextView.isVisible = false
        mStart1Button.isEnabled = false
        mStart1Button.isVisible = true
        mTurnOKButton.isEnabled = false
        mTurnOKButton.isVisible = true
        mTurnDoneTextView.isVisible = false
        mStart2Button.isEnabled = false
        mStart2Button.isVisible = true
        mStat1ProgressBar.isVisible = false
        mStatDone1TextView.isVisible = false
        mStat2ProgressBar.isVisible = false
        mStatDone2TextView.isVisible = false

        mTurn3OKButton.isEnabled = false
        mTurn3OKButton.isVisible = true
        mTurn3DoneTextView.isVisible = false
        mStart3Button.isEnabled = false
        mStart3Button.isVisible = true
        mStat3ProgressBar.isVisible = false
        mStatDone3TextView.isVisible = false
    }

    private fun endCalibration(){
    }


}


