package com.example.nhrmes.network

import com.example.nhrmes.Hospital
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    /**
     * Fetches real-time hospital data.
     * Replace "hospitals" with the actual endpoint of your data provider.
     */
    @GET("hospitals") 
    fun getHospitals(
        @Query("apiKey") apiKey: String
    ): Call<List<Hospital>>
}
