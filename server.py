import os
import random
from flask import Flask, request, jsonify, render_template_string
from flask_cors import CORS
from pymongo import MongoClient
from bson.objectid import ObjectId

app = Flask(__name__)
CORS(app)

# --- CONFIGURATION ---
MONGO_URI = "mongodb+srv://admin:M123456@cluster0.4vwg3eg.mongodb.net/?appName=Cluster0"
DB_NAME = "AdNetworkDB"
COLLECTION_ADS = "ads"

# --- HTML CODE (האתר מוטמע בתוך הקוד - זה יפתור את השגיאה מיד) ---
ADMIN_PAGE_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ad Management Portal</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f4f7f6; margin: 0; padding: 20px; }
        .container { max-width: 900px; margin: 0 auto; background: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
        h1 { color: #333; text-align: center; margin-bottom: 30px; }
        .form-group { margin-bottom: 15px; }
        label { display: block; margin-bottom: 5px; font-weight: bold; color: #555; }
        input { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 6px; box-sizing: border-box; }
        button { background-color: #4CAF50; color: white; padding: 12px 20px; border: none; border-radius: 6px; cursor: pointer; width: 100%; font-size: 16px; margin-top: 10px; transition: background 0.3s; }
        button:hover { background-color: #45a049; }
        table { width: 100%; border-collapse: collapse; margin-top: 40px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #eee; }
        th { background-color: #f8f9fa; color: #333; }
        img.preview { width: 50px; height: 50px; object-fit: cover; border-radius: 4px; }
        .stats { font-weight: bold; color: #2196F3; }
    </style>
</head>
<body>
    <div class="container">
        <h1>Ad Network Admin</h1>
        <div style="background: #f9f9f9; padding: 20px; border-radius: 8px;">
            <h3 style="margin-top: 0;">Create New Campaign</h3>
            <div class="form-group"><label>Ad Title:</label><input type="text" id="title" placeholder="Enter title"></div>
            <div class="form-group"><label>Image URL:</label><input type="text" id="imageUrl" placeholder="http://..."></div>
            <div class="form-group"><label>Target URL:</label><input type="text" id="targetUrl" placeholder="http://..."></div>
            <button onclick="createAd()">Create Ad</button>
        </div>
        <h3>Campaign Performance</h3>
        <table id="adsTable">
            <thead><tr><th>Preview</th><th>Title</th><th>Impressions</th><th>Clicks</th></tr></thead>
            <tbody></tbody>
        </table>
    </div>
    <script>
        const API_URL = "https://huskygotchi-project.onrender.com";
        async function loadAds() {
            try {
                const response = await fetch(`${API_URL}/admin/get-all-ads`);
                const ads = await response.json();
                const tbody = document.querySelector("#adsTable tbody");
                tbody.innerHTML = "";
                ads.forEach(ad => {
                    const row = `<tr>
                        <td><img src="${ad.imageUrl}" class="preview" onerror="this.src='https://via.placeholder.com/50'"></td>
                        <td>${ad.title}</td>
                        <td class="stats">${ad.impressions || 0}</td>
                        <td class="stats">${ad.clicks || 0}</td>
                    </tr>`;
                    tbody.innerHTML += row;
                });
            } catch (error) { console.error("Error loading ads:", error); }
        }
        async function createAd() {
            const title = document.getElementById('title').value;
            const imageUrl = document.getElementById('imageUrl').value;
            const targetUrl = document.getElementById('targetUrl').value;
            if (!title || !imageUrl) { alert("Please fill in all required fields"); return; }
            const data = { title, imageUrl, targetUrl };
            try {
                const response = await fetch(`${API_URL}/admin/create-ad`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                });
                if (response.ok) {
                    alert("Ad created successfully!");
                    document.getElementById('title').value = '';
                    document.getElementById('imageUrl').value = '';
                    document.getElementById('targetUrl').value = '';
                    loadAds();
                } else { alert("Failed to create ad"); }
            } catch (error) { alert("Network error: " + error); }
        }
        loadAds();
    </script>
</body>
</html>
"""

# --- DATABASE CONNECTION ---
try:
    client = MongoClient(MONGO_URI)
    db = client[DB_NAME]
    ads_collection = db[COLLECTION_ADS]
    print("Connected to MongoDB Atlas successfully!")
except Exception as e:
    print(f"Error connecting to MongoDB: {e}")

# --- ROUTES ---

@app.route('/', methods=['GET'])
def index():
    return jsonify({"status": "running", "message": "Ad Server is UP and Connected"})

# הפונקציה הזו מגישה את הטקסט מלמעלה - היא לא מחפשת שום קובץ ולכן לא יכולה להיכשל
@app.route('/admin')
def admin_page():
    return render_template_string(ADMIN_PAGE_HTML)

@app.route('/api/get-ad', methods=['GET'])
def get_ad():
    try:
        all_ads = list(ads_collection.find())
        if not all_ads:
            return jsonify({"error": "No ads found in DB"}), 404
        selected_ad = random.choice(all_ads)
        selected_ad['_id'] = str(selected_ad['_id'])
        return jsonify(selected_ad), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/impression', methods=['POST'])
def track_impression():
    try:
        data = request.json
        ad_id = data.get('ad_id')
        if not ad_id:
            return jsonify({"error": "Missing ad_id"}), 400
        ads_collection.update_one({"_id": ObjectId(ad_id)}, {"$inc": {"impressions": 1}})
        return jsonify({"message": "Impression recorded"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/click', methods=['POST'])
def track_click():
    try:
        data = request.json
        ad_id = data.get('ad_id')
        if not ad_id:
            return jsonify({"error": "Missing ad_id"}), 400
        ads_collection.update_one({"_id": ObjectId(ad_id)}, {"$inc": {"clicks": 1}})
        return jsonify({"message": "Click recorded"}), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# --- ADMIN API ROUTES ---

@app.route('/admin/create-ad', methods=['POST'])
def create_ad():
    try:
        data = request.json
        new_ad = {
            "title": data.get("title"),
            "imageUrl": data.get("imageUrl"),
            "targetUrl": data.get("targetUrl"),
            "impressions": 0,
            "clicks": 0
        }
        result = ads_collection.insert_one(new_ad)
        return jsonify({"message": "Ad created", "id": str(result.inserted_id)}), 201
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/admin/get-all-ads', methods=['GET'])
def get_all_ads():
    try:
        all_ads = list(ads_collection.find())
        for ad in all_ads:
            ad['_id'] = str(ad['_id'])
        return jsonify(all_ads), 200
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    port = int(os.environ.get("PORT", 5000))
    app.run(host='0.0.0.0', port=port)