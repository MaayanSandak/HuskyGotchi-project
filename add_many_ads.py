import requests

# The address of your local server
API_URL = "http://127.0.0.1:5000/admin/create-ad"

# List of ads to add
ads_list = [
    {
        "title": "Super Burger 🍔",
        "imageUrl": "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600",
        "targetUrl": "https://www.mcdonalds.com"
    },
    {
        "title": "Summer Vacation ✈️",
        "imageUrl": "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600",
        "targetUrl": "https://www.booking.com"
    },
    {
        "title": "Nike Air Max 👟",
        "imageUrl": "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600",
        "targetUrl": "https://www.nike.com"
    },
    {
        "title": "Gaming PC 🎮",
        "imageUrl": "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=600",
        "targetUrl": "https://store.steampowered.com"
    },
    {
        "title": "Delicious Sushi 🍣",
        "imageUrl": "https://images.unsplash.com/photo-1579871494447-9811cf80d66c?w=600",
        "targetUrl": "https://www.google.com"
    },
    {
        "title": "New iPhone 15 📱",
        "imageUrl": "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600",
        "targetUrl": "https://www.apple.com"
    }
]

print("Starting to add ads...")

for ad in ads_list:
    try:
        response = requests.post(API_URL, json=ad)
        if response.status_code == 201:
            print(f"Success! Added: {ad['title']}")
        else:
            print(f"Failed to add {ad['title']}: {response.text}")
    except Exception as e:
        print(f"Error connecting to server: {e}")
        break

print("Done! Restart your app to see random ads.")