package com.example.myadsdk

import com.google.gson.annotations.SerializedName

data class AdResponse(
    @SerializedName("_id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("targetUrl") val targetUrl: String
)