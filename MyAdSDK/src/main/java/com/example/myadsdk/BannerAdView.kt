package com.example.myadsdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class BannerAdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val imageView: ImageView = ImageView(context)
    private var currentAdId: String? = null

    // Updated IP address for physical device connection based on your screenshot
    private val BASE_URL = "http://192.168.1.130:5000/"

    private val apiService: AdApiService

    init {
        addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        // Start hidden
        visibility = View.GONE

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(AdApiService::class.java)

        setupClickListener()
    }

    private fun setupClickListener() {
        imageView.setOnClickListener {
            currentAdId?.let { id ->
                reportClick(id)
                Toast.makeText(context, "Opening Ad...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun loadAd() {
        apiService.getAd().enqueue(object : Callback<AdResponse> {
            override fun onResponse(call: Call<AdResponse>, response: Response<AdResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val ad = response.body()!!
                    currentAdId = ad.id
                    displayImage(ad.imageUrl)
                    setupTargetUrl(ad.targetUrl)
                    reportImpression(ad.id)

                    // Only show the ad view if we successfully loaded data
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }

            override fun onFailure(call: Call<AdResponse>, t: Throwable) {
                // If error occurs (e.g. server down), keep hidden
                visibility = View.GONE
            }
        })
    }

    private fun displayImage(url: String) {
        com.bumptech.glide.Glide.with(context)
            .load(url)
            .into(imageView)
    }

    private fun setupTargetUrl(url: String) {
        imageView.setOnClickListener {
            currentAdId?.let { id -> reportClick(id) }
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun reportImpression(adId: String) {
        apiService.reportImpression(AnalyticsRequest(adId)).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }

    private fun reportClick(adId: String) {
        apiService.reportClick(AnalyticsRequest(adId)).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {}
            override fun onFailure(call: Call<Void>, t: Throwable) {}
        })
    }
}