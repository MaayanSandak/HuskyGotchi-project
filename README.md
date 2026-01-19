# HuskyAds - Android Advertising SDK & Server 🐶

HuskyAds is a complete advertising solution for Android applications. It includes a backend API service for managing ads, an Android SDK for displaying ads, and an administration portal.

## 🚀 Project Components

### 1. API Service (Backend)
A RESTful API built with **Python Flask** and **MongoDB**.
- **Technology:** Python, Flask, PyMongo.
- **Database:** MongoDB Atlas (Cloud).
- **Features:** - Serve ads to the SDK.
  - Track impressions and clicks.
  - Admin API for creating campaigns.

### 2. Android SDK (Library)
A plug-and-play Android library that developers can integrate into their apps.
- **Technology:** Kotlin, Retrofit, Glide.
- **Key Feature:** `BannerAdView` - A custom view that handles ad fetching and display automatically.
- **Integration:** ```kotlin
    // Add to layout
    <com.example.myadsdk.BannerAdView
        android:id="@+id/bannerAdView"
        android:layout_width="match_parent"
        android:layout_height="80dp" />
    
    // Load in code
    bannerAdView.loadAd()
    ```

### 3. Administration Portal
A web-based dashboard for managing ad campaigns.
- **Features:**
  - Create new ads (Title, Image, Target URL).
  - View real-time analytics (Impressions & Clicks).

### 4. Example Application (HuskyGotchi)
A Tamagotchi-style game demonstrating the integration of the HuskyAds SDK.
- The game displays a banner ad at the bottom of the screen.
- Ads are fetched dynamically from the server.

---

## 🛠️ Setup Instructions

### Backend Setup
1. Navigate to the server folder.
2. Install dependencies:
   ```bash
   pip install flask flask-cors pymongo requests
