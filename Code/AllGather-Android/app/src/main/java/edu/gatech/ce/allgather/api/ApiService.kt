package edu.gatech.ce.allgather.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ApiService {
    @Multipart
    @POST("gathered_session_data")
    fun uploadGatheredData(@Part file: MultipartBody.Part): Call<ResponseBody>

    @Streaming
    @GET("{organization_id}/curve_inventory")
    fun fetchCurveInventory(@Path("organization_id") organizationId: Int): Call<ResponseBody>
}
