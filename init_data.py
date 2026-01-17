import requests
import time

URL = "http://127.0.0.1:5000/admin/create-ad"

ad1 = {
    "title": "Tasty Pizza",
    "imageUrl": "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=600",
    "targetUrl": "https://www.dominos.com"
}

ad2 = {
    "title": "VET4PET - Best Care",
    "imageUrl": "https://images.unsplash.com/photo-1583337130417-3346a1be7dee?w=600",
    "targetUrl": "https://www.vet4pet.com"
}

def upload_ad(ad_data):
    try:
        response = requests.post(URL, json=ad_data)
        if response.status_code == 201:
            print(f"Success! Created ad: {ad_data['title']}")
        else:
            print(f"Failed to create ad. Server said: {response.text}")
    except Exception as e:
        print(f"Error: {e}")

time.sleep(1)
upload_ad(ad1)
upload_ad(ad2)