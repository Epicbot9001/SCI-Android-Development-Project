package edu.gatech.ce.allgather.api

import android.os.Environment
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

// Creating Singletons of RetrofitClient and ApiClient

fun getServerUrl(): String {
    val documentsDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    val configFile = File(documentsDir, "config.json")

    // Check if file exists
    if (!configFile.exists()) {
        println("Config file not found.")
        return ""
    }

    // Read the file contents
    val fis = FileInputStream(configFile)
    val reader = BufferedReader(InputStreamReader(fis))
    val builder = StringBuilder()
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        builder.append(line)
    }
    reader.close()

    // Parse the JSON and return it

    // Fetch the key url in the jsonObj
    val url = JSONObject(builder.toString()).getString("url")

    Log.d("BASE_URL 123", url)

    return url
}

object RetrofitClient {
//    private const val BASE_URL = "https://qualification-andorra-furniture-question.trycloudflare.com/api/v1/"
    private val BASE_URL = getServerUrl()

    private val client = OkHttpClient.Builder().also { client ->
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        client.addInterceptor(logging)
    }.build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }
}

object ApiClient {
    val apiService: ApiService by lazy {
        RetrofitClient.retrofit.create(ApiService::class.java)
    }
}