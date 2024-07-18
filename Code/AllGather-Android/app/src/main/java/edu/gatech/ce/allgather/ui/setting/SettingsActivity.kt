package edu.gatech.ce.allgather.ui.setting

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.preference.*
import androidx.core.app.NavUtils
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import edu.gatech.ce.allgather.AllGatherApplication
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.old.helpers.CameraHelper
import edu.gatech.ce.allgather.old.utils.StorageUtil

/**
 * Created by andyleekung on 9/6/17.
 *
 * A [PreferenceActivity] that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 *
 * See [Android Design: Settings](http://developer.android.com/design/patterns/settings.html)
 * for design guidelines and the [Settings API Guide](http://developer.android.com/guide/topics/ui/settings.html)
 * for more information on developing a Settings UI.
 */
class SettingsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()
    }

    override fun onResume() {
        super.onResume()
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onNavigateUp(): Boolean {
        NavUtils.navigateUpFromSameTask(this)
        return true
    }

    fun onOptionsItemSelected(item: MenuItem?): Boolean? {
        when (item?.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return item?.let { super.onOptionsItemSelected(it) }
    }

    /**
     * {@inheritDoc}
     */
    override fun onIsMultiPane(): Boolean {
        return isXLargeTablet(this)
    }

    /**
     * {@inheritDoc}
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onBuildHeaders(target: List<PreferenceActivity.Header>) {
        loadHeadersFromResource(R.xml.pref_headers, target)
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || GeneralPreferenceFragment::class.java.name == fragmentName
                || CameraPreferenceFragment::class.java.name == fragmentName
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class GeneralPreferenceFragment : PreferenceFragment() {
        lateinit var strUtil: StorageUtil
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_general)
            setHasOptionsMenu(true)
            // Get the Storage Utility
            val application = (activity as SettingsActivity).application as AllGatherApplication
            strUtil = application.storageUtil

            val storagePreference = findPreference(resources.getString(R.string.pref_storage_list)) as ListPreference
            setStorageListPreferenceData(storagePreference)

            val driverIdPreference = findPreference(resources.getString(R.string.pref_driverid)) as EditTextPreference

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(storagePreference)
            bindPreferenceSummaryToValue(driverIdPreference)

            // set a default value
            if (storagePreference.summary == null) {
                // if there is an SD card set that as default
                // Hack? SD card at index 1?
                val entryValues = storagePreference.entryValues
                storagePreference.value = if (storagePreference.entryValues.size > 1)
                    entryValues[1].toString() else entryValues[0].toString()
                storagePreference.summary = storagePreference.entry
            }
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }

        private fun setStorageListPreferenceData(listPreference: ListPreference) {
            val storageLocations = strUtil.getPossibleStoragePaths()
            // Type of storage e.g. Internal, SD Card
            val entries = storageLocations.map { it.component2().type }.toTypedArray()
            // TODO explore better values?
            val entryValues = storageLocations.map { it.component1() }.toTypedArray()
            listPreference.entries = entries
            listPreference.entryValues = entryValues

        }
    }

    /**
     * This fragment shows camera preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    class CameraPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_camera)
            setHasOptionsMenu(true)

            val resolutionPreference = findPreference(resources.getString(R.string.pref_resolution)) as ListPreference
            val frameratePreference = findPreference(resources.getString(R.string.pref_framerate)) as ListPreference
            val bitratePreference = findPreference(resources.getString(R.string.pref_bitrate)) as ListPreference
            val whiteBalancePreference = findPreference(resources.getString(R.string.pref_white_balance)) as CheckBoxPreference
            val autofocusPreference = findPreference(resources.getString(R.string.pref_autofocus)) as CheckBoxPreference
            val antibandingPreference = findPreference(resources.getString(R.string.pref_antibanding)) as CheckBoxPreference

            setResolutionPreferenceData(resolutionPreference)

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(resolutionPreference)
            bindPreferenceSummaryToValue(frameratePreference)
            bindPreferenceSummaryToValue(bitratePreference)
//
            // set a default value
            if (resolutionPreference.summary == null) {
                // todo pick biggest?
                val entryValues = resolutionPreference.entryValues
                resolutionPreference.value = entryValues[0].toString()
                resolutionPreference.summary = resolutionPreference.entry
            }

            if (frameratePreference.summary == null) {
                val entryValues = frameratePreference.entryValues
                // 10 default
                frameratePreference.value = entryValues[1].toString()
                frameratePreference.summary = frameratePreference.entry
            }

            if (bitratePreference.summary == null) {
                val entryValues = bitratePreference.entryValues
                bitratePreference.value = entryValues[0].toString()
                bitratePreference.summary = bitratePreference.entry
            }

        }

        private fun setResolutionPreferenceData(listPreference: ListPreference) {
            val videoSizes = CameraHelper.getSupportedSizes(this.activity)
            // Type of storage e.g. Internal, SD Card
            val entries = videoSizes.map {it.toString()}.toTypedArray()
            // TODO explore better values?
            val entryValues = videoSizes.map {it.toString()}.toTypedArray()
            listPreference.entries = entries
            listPreference.entryValues = entryValues

        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }

    }

    companion object {

        private val TAG = "SettingsActivity"

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val listPreference = preference
                val index = listPreference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                        if (index >= 0)
                            listPreference.entries[index]
                        else
                            null)
            } else if (preference is EditTextPreference) {
                if (preference.key == preference.context.resources.getString(R.string.pref_driverid)) {
                    Log.d(TAG, "onPreferenceChangeListener")
                    if (stringValue.length !in 6..8) {
                        Toast.makeText(preference.context, R.string.cannot_proceed, Toast.LENGTH_LONG).show()
                        return@OnPreferenceChangeListener false
                    }
                }
                preference.summary = stringValue
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.summary = stringValue
            }
            true
        }

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.

         * @see .sBindPreferenceSummaryToValueListener
         */
        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.context)
                            .getString(preference.key, ""))
        }
    }
}
