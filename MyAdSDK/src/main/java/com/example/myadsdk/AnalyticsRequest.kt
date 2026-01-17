package com.example.myadsdk

import com.google.gson.annotations.SerializedName

data class AnalyticsRequest(
    @SerializedName("ad_id") val adId: String
)