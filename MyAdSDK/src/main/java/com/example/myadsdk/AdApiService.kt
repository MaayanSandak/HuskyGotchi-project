package com.example.myadsdk

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AdApiService {

    @GET("/api/get-ad")
    fun getAd(): Call<AdResponse>

    @POST("/api/impression")
    fun reportImpression(@Body request: AnalyticsRequest): Call<Void>

    @POST("/api/click")
    fun reportClick(@Body request: AnalyticsRequest): Call<Void>
}