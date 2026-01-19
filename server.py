import os
import random
from flask import Flask, jsonify, request
from flask_cors import CORS
from pymongo import MongoClient
from bson.objectid import ObjectId

app = Flask(__name__)
CORS(app)

# --- CONFIGURATION ---
# Your specific connection string
MONGO_URI = "mongodb+srv://admin:M123456@cluster0.4vwg3eg.mongodb.net/?appName=Cluster0"
DB_NAME = "AdNetworkDB"
COLLECTION_ADS = "ads"

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


@app.route('/api/get-ad', methods=['GET'])
def get_ad():
    try:
        # Fetch all ads from the cloud database
        all_ads = list(ads_collection.find())

        if not all_ads:
            return jsonify({"error": "No ads found in DB"}), 404

        # Select a random ad
        selected_ad = random.choice(all_ads)

        # Convert ObjectId to string
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

        ads_collection.update_one(
            {"_id": ObjectId(ad_id)},
            {"$inc": {"impressions": 1}}
        )
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

        ads_collection.update_one(
            {"_id": ObjectId(ad_id)},
            {"$inc": {"clicks": 1}}
        )
        return jsonify({"message": "Click recorded"}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500


# --- ADMIN ROUTE ---
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
    # Get the port from the environment variable or use 5000 as default
    port = int(os.environ.get("PORT", 5000))
    # Disable debug mode for production
    app.run(host='0.0.0.0', port=port)
