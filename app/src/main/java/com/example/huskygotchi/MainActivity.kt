package com.example.huskygotchi

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.myadsdk.BannerAdView // Import the Ad SDK

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

    // UI
    private lateinit var pbHunger: ProgressBar
    private lateinit var pbEnergy: ProgressBar
    private lateinit var pbHappiness: ProgressBar
    private lateinit var pbCleanliness: ProgressBar
    private lateinit var ivHusky: ImageView
    private lateinit var tvMessage: TextView
    private lateinit var layoutActions: View
    private lateinit var btnRestart: Button

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI Components
        pbHunger = findViewById(R.id.pbHunger)
        pbEnergy = findViewById(R.id.pbEnergy)
        pbHappiness = findViewById(R.id.pbHappiness)
        pbCleanliness = findViewById(R.id.pbCleanliness)
        ivHusky = findViewById(R.id.ivHusky)
        tvMessage = findViewById(R.id.tvMessage)
        layoutActions = findViewById(R.id.layoutActions)
        btnRestart = findViewById(R.id.btnRestart)

        // Initialize Ad SDK
        bannerAdView = findViewById(R.id.bannerAdView)
        bannerAdView.loadAd() // Fetch the ad from server

        // Buttons setup
        findViewById<Button>(R.id.btnFeed).setOnClickListener {
            performAction(it, "eating")
        }

        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            performAction(it, "playing")
        }

        findViewById<Button>(R.id.btnClean).setOnClickListener {
            performAction(it, "cleaning")
        }

        findViewById<Button>(R.id.btnSleep).setOnClickListener {
            if (!isGameRunning || isSleeping) return@setOnClickListener
            animateButton(it)
            isSleeping = true
            showGameMessage("Shhh... Goodnight 💤")
            updateUI()
        }

        btnRestart.setOnClickListener {
            restartGame()
        }

        handler.post(gameRunnable)
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
                showGameMessage("Yummy! 🍖")
            }
            "playing" -> {
                happiness = (happiness + 20).coerceAtMost(100)
                energy = (energy - 10).coerceAtLeast(0)
                hunger = (hunger - 10).coerceAtLeast(0)
                cleanliness = (cleanliness - 10).coerceAtLeast(0)
                ivHusky.setImageResource(R.drawable.playing_dog)
                showGameMessage("So much fun! 🎾")
            }
            "cleaning" -> {
                cleanliness = 100
                happiness = (happiness + 5).coerceAtMost(100)
                ivHusky.setImageResource(R.drawable.washing_dog)
                showGameMessage("Sparkling clean! ✨")
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
                showGameMessage("Good morning! ☀️")
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
            showGameMessage("Oh no! Game Over 💀")
            updateUI()
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

        // Optional: Load a new ad when game restarts
        bannerAdView.loadAd()

        updateUI()
        handler.post(gameRunnable)
    }

    private fun animateButton(view: View) {
        view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }
}