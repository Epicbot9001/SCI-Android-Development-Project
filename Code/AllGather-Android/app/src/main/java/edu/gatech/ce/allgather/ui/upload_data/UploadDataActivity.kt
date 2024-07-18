package edu.gatech.ce.allgather.ui.upload_data

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckedTextView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import edu.gatech.ce.allgather.AllGatherApplication
import edu.gatech.ce.allgather.base.BaseActivity
import edu.gatech.ce.allgather.R
import edu.gatech.ce.allgather.api.ApiClient
import edu.gatech.ce.allgather.api.Post
import edu.gatech.ce.allgather.utils.zipFolder
import edu.gatech.ce.allgather.utils.zipFolderNoMp4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class UploadDataActivity : BaseActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_data)

        // Get the folders from the storage and display them
        val storageUtil = (application as AllGatherApplication).storageUtil
        val dataFolderPaths = storageUtil.listSessionFolders()

        if (dataFolderPaths == null) {
            Toast.makeText(this, "No data to upload", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        //get shared preferences for uploaded files
        val uploadedFiles = getSharedPreferences("uploadedFiles", MODE_PRIVATE)
        val uploadedFilesEdit = uploadedFiles.edit()

        val emptySet: MutableSet<String> = mutableSetOf()
        val prevUploaded = uploadedFiles.getStringSet("uploadedFiles", emptySet)

        //prepare listviews and datafolders
        val dataFolders = dataFolderPaths.map({ it.name.substringAfterLast("/") }).toMutableList()
        val fileListView = findViewById<ListView>(R.id.files_list)
        val uploadedFileListView = findViewById<ListView>(R.id.uploaded_files_list)

        //Map files to their sizes
        val fileSizeMap = HashMap<String, String>()
        val fileSizeMapUploaded = HashMap<String, String>()

        //Separate paths between uploaded files and non uploaded files
        val uploadedFilePaths: MutableList<String> = mutableListOf()
        val notUploadedFilePaths: MutableList<String> = mutableListOf()

        dataFolders.forEachIndexed { index, folder ->
            val path = dataFolderPaths[index].absolutePath
            val file = File(path)
            val fileSize = getFolderSize(file)/1024
            if (prevUploaded != null) {
                if (prevUploaded.contains(path)) {
                    fileSizeMapUploaded[folder] = "$fileSize KB"
                    uploadedFilePaths.add(path)
                } else {
                    fileSizeMap[folder] = "$fileSize KB"
                    notUploadedFilePaths.add(path)
                }
            } else {
                fileSizeMap[folder] = "$fileSize KB"
                notUploadedFilePaths.add(path)
            }
        }

        //Convert hashmap to adapter and display on the list
//        val data = fileSizeMap.entries.map { mapOf("key" to it.key, "value" to it.value) }

//        val displayedData = fileSizeMap.entries.map { "${it.key} \nSize: ${it.value}" }

        val displayedData = dataFolders
            .filter { fileSizeMap[it]!= null }
            .map { "$it \nSize: ${fileSizeMap[it]}"}

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, displayedData)

        val displayedDataUploaded = dataFolders
            .filter { fileSizeMapUploaded[it]!= null }
            .map { "$it \nSize: ${fileSizeMapUploaded[it]}"}

        val adapterUploaded = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, displayedDataUploaded)


        //file and button views
        fileListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        fileListView.adapter = adapter

        uploadedFileListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        uploadedFileListView.adapter = adapterUploaded

        val checkedItemPositions = fileListView.checkedItemPositions
        val checkedUploadedItemPositions = uploadedFileListView.checkedItemPositions

        val uploadData = findViewById<Button>(R.id.select_files_button)

        val uploadDataWithoutmp4 = findViewById<Button>(R.id.select_files_button_no_mp4)

        val uploadedFilesText = findViewById<TextView>(R.id.uploaded_files_text)
        val uploadedFilesList = findViewById<ListView>(R.id.uploaded_files_list)

        uploadedFilesList.visibility = View.GONE
        uploadedFilesText.setOnClickListener {
            if (uploadedFilesList.visibility == View.GONE) {
                uploadedFilesList.visibility = View.VISIBLE
            } else {
                uploadedFilesList.visibility = View.GONE
            }
        }


        //local helper functions for buttons
        fun checkEmptyList(): Boolean {
            for (i in 0 until fileListView.checkedItemPositions.size()) {
                val isChecked = fileListView.checkedItemPositions.valueAt(i)
                if (isChecked) {
                    return true
                }
            }
            for (i in 0 until uploadedFileListView.checkedItemPositions.size()) {
                val isChecked = uploadedFileListView.checkedItemPositions.valueAt(i)
                if (isChecked) {
                    return true
                }
            }
            return false
        }
        fun uploadDataButtonFunction(mp4: Boolean) {
            val uploadedFileSet: MutableSet<String>? = prevUploaded?.toMutableSet()
            val toUpload = mutableListOf<String>()
            for (i in 0 until checkedItemPositions.size()) {
                val isChecked = checkedItemPositions.valueAt(i)

                if (isChecked) {
                    val position = checkedItemPositions.keyAt(i)
                    val folderPath = notUploadedFilePaths[position]

                    uploadedFileSet?.add(folderPath)
                    toUpload.add(folderPath)

                }
            }
            uploadedFilesEdit.apply {
                putStringSet("uploadedFiles", uploadedFileSet)
                apply()
            }


            for (i in 0 until checkedUploadedItemPositions.size()) {
                val isChecked = checkedUploadedItemPositions.valueAt(i)

                if (isChecked) {
                    val position = checkedUploadedItemPositions.keyAt(i)
                    val folderPath = uploadedFilePaths[position]

                    toUpload.add(folderPath)

                }
            }
            for (uploadFile in toUpload) {
                println("before $uploadFile")
                uploadData(uploadFile, mp4)
            }

        }


        //button functionality
        uploadData.setOnClickListener {
            //check if any items are selected first
            var anySelected = checkEmptyList()

            if (!anySelected) {
                Toast.makeText(this, "No data selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //upload data
            uploadDataButtonFunction(true)

            finish()
            return@setOnClickListener

        }

        uploadDataWithoutmp4.setOnClickListener {
            //check if any items are selected first
            var anySelected = checkEmptyList()

            if (!anySelected) {
                Toast.makeText(this, "No data selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //upload data
            uploadDataButtonFunction(false)

            finish()
            return@setOnClickListener

        }


    }



    //Helper function to get the size of the folders
    fun getFolderSize(f: File): Long {
        var size: Long = 0
        if (f.isDirectory) {
            val files = f.listFiles()
            if (files != null) {
                for (file in files) {
                    size += getFolderSize(file)
                }
            }
        } else {
            size = f.length()
        }
        return size
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun uploadData(folderPath: String, useMp4: Boolean) {
        // Turn on the progress bar
        val progressBar = findViewById<ProgressBar>(R.id.dataUploadProgressBar)
        progressBar.visibility = View.VISIBLE

        // Step 1: Compress the folder into a ZIP file
        val zipFilePath = "$folderPath.zip"
        if (useMp4) {
            zipFolder(folderPath, zipFilePath)
        } else {
            zipFolderNoMp4(folderPath, zipFilePath)
        }

        // Step 2: Prepare the ZIP file for uploading
        val zipFile = File(zipFilePath)
        Log.d(TAG, "Size of session zip file is ${zipFile.length()} bytes")
        val requestFile = zipFile.asRequestBody("application/zip".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", zipFile.name, requestFile)

        // Step 3: Upload the file
        val call = ApiClient.apiService.uploadGatheredData(body)
        call.enqueue(object : Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@UploadDataActivity, "${zipFile.name} uploaded successfully", Toast.LENGTH_SHORT).show()

                    Log.d(TAG, "Folder ${zipFile.name} has been uploaded successfully")
                } else {
                    Log.d(TAG, "File upload failed: ${response.message()}")
                }

                // Delete the ZIP file as it serves no purpose now
                zipFile.delete()

                // Turn off the progress bar
                progressBar.visibility = View.GONE

                finish()
                return
            }

            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                Log.e(TAG, "File upload error: ${t.message}")

                //Until there is a new webserver, always delete the zipfile
                zipFile.delete()
                finish()
                return
            }
        })
    }
}

