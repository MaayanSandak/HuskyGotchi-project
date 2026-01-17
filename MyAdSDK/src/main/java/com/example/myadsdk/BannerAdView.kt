package com.example.myadsdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
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

    // TODO: Verify this is your computer's IP
    private val BASE_URL = "http://192.168.1.130:5000/"

    private val apiService: AdApiService

    // Timer handling for auto-refresh
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadAd() // Load a new ad
            refreshHandler.postDelayed(this, 10000) // Schedule next run in 10 seconds
        }
    }

    init {
        addView(imageView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        // Start hidden until first ad loads
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

                    // Only update if it's a different ad (optional check)
                    if (currentAdId != ad.id) {
                        currentAdId = ad.id
                        displayImage(ad.imageUrl)
                        setupTargetUrl(ad.targetUrl)
                        reportImpression(ad.id)

                        // Show the view with animation
                        alpha = 0f
                        visibility = View.VISIBLE
                        animate().alpha(1f).setDuration(500).start()
                    }
                }
            }

            override fun onFailure(call: Call<AdResponse>, t: Throwable) {
                // Keep hidden on error
            }
        })
    }

    // Start auto-refresh when the view is displayed on screen
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        refreshRunnable.run() // Start the loop
    }

    // Stop auto-refresh when the view is removed (to save battery)
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        refreshHandler.removeCallbacks(refreshRunnable) // Stop the loop
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