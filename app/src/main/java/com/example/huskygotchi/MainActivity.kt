package com.example.huskygotchi

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myadsdk.BannerAdView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    // Stats
    private var hunger = 100
    private var energy = 100
    private var happiness = 100
    private var cleanliness = 100

    // State
    private var isGameRunning = true
    private var isSleeping = false
    private var isPerformingAction = false
    private var isPremiumUser = false

    // Firebase
    private lateinit var firebaseAnalytics: FirebaseAnalytics


    // UI
    private lateinit var pbHunger: ProgressBar
    private lateinit var pbEnergy: ProgressBar
    private lateinit var pbHappiness: ProgressBar
    private lateinit var pbCleanliness: ProgressBar
    private lateinit var ivHusky: ImageView
    private lateinit var tvMessage: TextView
    private lateinit var layoutActions: View
    private lateinit var btnRestart: Button
    private lateinit var btnSettings: ImageButton

    // Ad SDK View
    private lateinit var bannerAdView: BannerAdView

    private val handler = Handler(Looper.getMainLooper())
    private val gameRunnable = object : Runnable {
        override fun run() {
            if (isGameRunning) {
                decreaseStats()
                updateUI()
                checkGameOver()
                handler.postDelayed(this, 2000)
            }
        }
    }

    // Permission Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notifications Denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseAnalytics = Firebase.analytics

        // 1. Load Premium Status
        loadData()

        // 2. Permissions
        checkNotificationPermission()

        // 3. UI Setup
        pbHunger = findViewById(R.id.pbHunger)
        pbEnergy = findViewById(R.id.pbEnergy)
        pbHappiness = findViewById(R.id.pbHappiness)
        pbCleanliness = findViewById(R.id.pbCleanliness)
        ivHusky = findViewById(R.id.ivHusky)
        tvMessage = findViewById(R.id.tvMessage)
        layoutActions = findViewById(R.id.layoutActions)
        btnRestart = findViewById(R.id.btnRestart)
        btnSettings = findViewById(R.id.btnSettings)

        // Settings Button Logic
        btnSettings.setOnClickListener { showSettingsDialog() }

        // 4. Initialize Ads
        bannerAdView = findViewById(R.id.bannerAdView)
        updateBannerVisibility() // Force check immediately

        // 5. Auto-Rate Dialog
        handler.postDelayed({
            if (isGameRunning) showRateDialog()
        }, 8000)

        // 6. Game Buttons
        findViewById<Button>(R.id.btnFeed).setOnClickListener {
            performAction(it, "eating")
            sendAnalytics("action_feed")
        }

        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            performAction(it, "playing")
            sendAnalytics("action_play")
        }

        findViewById<Button>(R.id.btnClean).setOnClickListener {
            performAction(it, "cleaning")
            sendAnalytics("action_clean")
        }

        findViewById<Button>(R.id.btnSleep).setOnClickListener {
            if (!isGameRunning || isSleeping) return@setOnClickListener
            animateButton(it)
            isSleeping = true
            showGameMessage("Shhh... Goodnight \uD83D\uDCA4")
            sendAnalytics("action_sleep")
            updateUI()
        }

        btnRestart.setOnClickListener {
            restartGame()
            sendAnalytics("game_restart")
        }

        handler.post(gameRunnable)
        sendAnalytics("app_opened")
    }

    // --- Premium & Ads Logic ---

    private fun loadData() {
        val prefs = getSharedPreferences("HuskyGamePrefs", MODE_PRIVATE)
        isPremiumUser = prefs.getBoolean("is_premium", false)
    }

    private fun savePremiumStatus(isPremium: Boolean) {
        val prefs = getSharedPreferences("HuskyGamePrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("is_premium", isPremium).apply()

        isPremiumUser = isPremium
        updateBannerVisibility() // Update immediately
    }

    private fun updateBannerVisibility() {
        if (isPremiumUser) {
            // Hide Ad
            bannerAdView.visibility = View.GONE
            val params = bannerAdView.layoutParams
            params.height = 0
            bannerAdView.layoutParams = params
        } else {
            // Show Ad
            bannerAdView.visibility = View.VISIBLE
            val params = bannerAdView.layoutParams
            params.height = 250 // Restore height
            bannerAdView.layoutParams = params
            bannerAdView.loadAd()
        }
    }

    // --- REWARDED AD LOGIC (The Square Popup) ---
    private fun showRealAdPopup() {
        val dialog = Dialog(this)
        dialog.setCancelable(false)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(Color.WHITE)
        layout.setPadding(50, 50, 50, 50)

        val title = TextView(this)
        title.text = "Watch Ad for Energy..."
        title.textSize = 18f
        title.gravity = Gravity.CENTER
        layout.addView(title)

        val popupAdView = BannerAdView(this)
        val params = LinearLayout.LayoutParams(800, 800)
        params.setMargins(0, 30, 0, 30)
        popupAdView.layoutParams = params
        layout.addView(popupAdView)

        val subTitle = TextView(this)
        subTitle.text = "(Closing in 5 seconds...)"
        subTitle.gravity = Gravity.CENTER
        layout.addView(subTitle)

        dialog.setContentView(layout)
        dialog.show()

        popupAdView.loadAd()

        handler.postDelayed({
            if (dialog.isShowing) {
                dialog.dismiss()
                energy = 100
                updateUI()
                Toast.makeText(this, "Energy Fully Refilled! \u26A1", Toast.LENGTH_LONG).show()
                sendAnalytics("ad_rewarded_completed")
            }
        }, 5000)
    }

    // --- Dialogs ---
    private fun showSettingsDialog() {
        val options = arrayOf("Buy Premium ($10)", "Share App", "Rate Us", "Get Free Energy (Ad)", "Reset to Free (Dev)")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Husky Settings \u2699\uFE0F")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> { // Buy Premium
                    savePremiumStatus(true)
                    Toast.makeText(this, "Premium Purchased! Ads Removed.", Toast.LENGTH_SHORT).show()
                    sendAnalytics("monetization_purchase")
                }
                1 -> { // Share
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, "Check out my HuskyGotchi! It's awesome! \uD83D\uDC15")
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(sendIntent, "Share via"))
                    sendAnalytics("viral_share")
                }
                2 -> { // Rate
                    showRateDialog()
                }
                3 -> { // Watch Ad
                    // FIX: Always show ad popup, even if premium
                    showRealAdPopup()
                }
                4 -> { // RESET TO FREE (Dev)
                    savePremiumStatus(false)
                    Toast.makeText(this, "Reset to Free User. Ads Restored.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.show()
    }

    private fun showRateDialog() {
        AlertDialog.Builder(this)
            .setTitle("Rate Us")
            .setMessage("Do you like playing with Husky? Please rate us 5 stars!")
            .setPositiveButton("⭐️⭐️⭐️⭐️⭐️") { _, _ ->
                Toast.makeText(this, "Thank you for the love!", Toast.LENGTH_SHORT).show()
                sendAnalytics("viral_rate_5_stars")
            }
            .setNegativeButton("No") { _, _ -> }
            .show()
    }

    // --- Helper Methods ---
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        scheduleNotification()
    }

    private fun scheduleNotification() {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }

    private fun sendAnalytics(eventName: String) {
        val bundle = Bundle()
        bundle.putString("action_type", eventName)
        firebaseAnalytics.logEvent("user_interaction", bundle)

        Thread {
            try {
                val url = URL("http://10.0.2.2:5000/log?event=$eventName")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 2000
                conn.connect()
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun performAction(view: View, actionType: String) {
        if (!isGameRunning || isSleeping || isPerformingAction) return

        animateButton(view)
        isPerformingAction = true

        when (actionType) {
            "eating" -> {
                hunger = (hunger + 20).coerceAtMost(100)
                cleanliness = (cleanliness - 5).coerceAtLeast(0)
                ivHusky.setImageResource(R.drawable.eating_dog)
                showGameMessage("Yummy! \uD83C\uDF56")
            }
            "playing" -> {
                happiness = (happiness + 20).coerceAtMost(100)
                energy = (energy - 10).coerceAtLeast(0)
                hunger = (hunger - 10).coerceAtLeast(0)
                cleanliness = (cleanliness - 10).coerceAtLeast(0)
                ivHusky.setImageResource(R.drawable.playing_dog)
                showGameMessage("So much fun! \uD83C\uDFBE")
            }
            "cleaning" -> {
                cleanliness = 100
                happiness = (happiness + 5).coerceAtMost(100)
                ivHusky.setImageResource(R.drawable.washing_dog)
                showGameMessage("Sparkling clean! \u2728")
            }
        }

        handler.postDelayed({
            isPerformingAction = false
            updateUI()
        }, 1000)
    }

    private fun decreaseStats() {
        if (isSleeping) {
            energy = (energy + 10).coerceAtMost(100)
            hunger = (hunger - 5).coerceAtLeast(0)
            if (energy == 100) {
                isSleeping = false
                showGameMessage("Good morning! \u2600\uFE0F")
            }
        } else {
            hunger = (hunger - 2).coerceAtLeast(0)
            energy = (energy - 2).coerceAtLeast(0)
            happiness = (happiness - 2).coerceAtLeast(0)
            cleanliness = (cleanliness - 2).coerceAtLeast(0)
        }
    }

    private fun updateUI() {
        if (isPerformingAction) {
            pbHunger.progress = hunger
            pbEnergy.progress = energy
            pbHappiness.progress = happiness
            pbCleanliness.progress = cleanliness
            return
        }

        pbHunger.progress = hunger
        pbEnergy.progress = energy
        pbHappiness.progress = happiness
        pbCleanliness.progress = cleanliness

        if (!isGameRunning) {
            ivHusky.setImageResource(R.drawable.game_over_dog)
            return
        }

        if (isSleeping) {
            ivHusky.setImageResource(R.drawable.sleeping_dog)
            return
        }

        if (hunger < 30) {
            ivHusky.setImageResource(R.drawable.hungry_dog)
        } else if (energy < 30) {
            ivHusky.setImageResource(R.drawable.tired_dog)
        } else if (cleanliness < 30) {
            ivHusky.setImageResource(R.drawable.dirty_dog)
        } else if (happiness < 30) {
            ivHusky.setImageResource(R.drawable.sick_dog)
        } else {
            ivHusky.setImageResource(R.drawable.happy_dog)
        }
    }

    private fun checkGameOver() {
        if (hunger == 0 || happiness == 0 || cleanliness == 0 || (energy == 0 && !isSleeping)) {
            isGameRunning = false
            layoutActions.visibility = View.GONE
            btnRestart.visibility = View.VISIBLE
            showGameMessage("Oh no! Game Over \uD83D\uDC80")
            updateUI()
            sendAnalytics("game_over")
        }
    }

    private fun showGameMessage(text: String) {
        tvMessage.text = text
        tvMessage.visibility = View.VISIBLE
        tvMessage.alpha = 1f

        tvMessage.animate().alpha(0f).setDuration(500).setStartDelay(1000).withEndAction {
            tvMessage.visibility = View.INVISIBLE
        }.start()
    }

    private fun restartGame() {
        hunger = 100
        energy = 100
        happiness = 100
        cleanliness = 100
        isGameRunning = true
        isSleeping = false
        layoutActions.visibility = View.VISIBLE
        btnRestart.visibility = View.GONE

        updateBannerVisibility()

        updateUI()
        handler.post(gameRunnable)
    }

    private fun animateButton(view: View) {
        view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }
}