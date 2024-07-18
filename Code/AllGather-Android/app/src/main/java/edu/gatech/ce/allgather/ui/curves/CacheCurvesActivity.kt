package edu.gatech.ce.allgather.ui.curves

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.api.ApiClient
import edu.gatech.ce.allgather.base.BaseActivity
import edu.gatech.ce.allgather.utils.unzip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.apache.commons.compress.archivers.zip.ZipFile
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class CacheCurvesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make API call the get the curves data as a zip file
        // TODO: Organization ID is currently hardcoded, make it as input later on.
        val organizationId = 2

        val call = ApiClient.apiService.fetchCurveInventory(organizationId)
        call.enqueue(object : Callback<ResponseBody> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Server contacted and has file");

                    CoroutineScope(Dispatchers.IO).launch {
                        // Save the zip content to temporary file as data.zip
                        val tempFile = File.createTempFile("curve_data", "zip")
                        response.body()?.byteStream()?.copyTo(tempFile.outputStream())
                        Log.d(TAG, "file download was successful")

                        // Load the curves from the temporary file
                        val zipFile = ZipFile.builder().setFile(tempFile).get()
                        // Load the file data.json
                        val dataFile = zipFile.getEntry("data.json")
                        val data = zipFile.getInputStream(dataFile).bufferedReader().use { it.readText() }
                        Log.d(TAG, "Unzipped file: $data")
                        // Now delete the zip file
                        tempFile.delete()

                        // Save this content to shared preference
                        val mSharedPref = PreferenceManager.getDefaultSharedPreferences(this@CacheCurvesActivity)
                        with (mSharedPref.edit()) {
                            putString("curve_data", data)
                            apply()
                        }

                        // Log the curve information
                        Log.d(TAG, "Curve data: $data")

                        // Now we can proceed to the main activity
                        finish()
                    }
                } else {
                    Log.d(TAG, "Server contact failed: ${response.message()}");
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.d(TAG, "Server contact failed: ${t.message}");
            }
        })
    }
}
