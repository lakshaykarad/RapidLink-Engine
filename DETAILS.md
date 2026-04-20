# 🗺️ Engineering Log 1: Solving GPS Drift and Multipath Errors

> *Below is a breakdown of the GNSS hardware challenges faced during physical device testing and the custom spatial filters written to solve them.*

<details>
<summary><b>🚨 The Problem: The "Scribble" Effect</b></summary>

While testing the application on a physical device, I noticed a significant issue with the tracking line. When I was moving fast, the line was straight. But when I stood still or walked very slowly, the app drew a massive, zig-zagging "scribble" all over the map.

This happens because of **GPS Multipath Error**. The signals from GPS satellites bounce off nearby buildings or trees before hitting the phone's antenna. Because the signal took a longer path, the phone's hardware miscalculates the distance and thinks the user suddenly jumped 5 to 10 meters away, even if they are standing completely still. 



Because my background service was logging every single one of these jumps, it created a messy UI and filled the local Room Database with useless data.
</details>

<details>
<summary><b>🤔 The Alternative: Why I didn't use the easy fix</b></summary>

The industry standard solution for this problem is to use Google's `FusedLocationProviderClient` (FLPC). The FLPC algorithm automatically combines GPS, Wi-Fi, and accelerometer data to figure out if you are actually moving, and it smooths out the line for you.

However, the core goal of my project was to build a system with **0% Google dependency**. Using FLPC would mean relying on Google Play Services for the most important part of my app. 

Therefore, I decided to use the raw Android `LocationManager` and build my own spatial data filters to clean up the satellite noise manually.
</details>

<details>
<summary><b>💡 The Solution: Custom Spatial Filters</b></summary>

To fix the drift, I implemented a strict filtering system inside my `onLocationChanged` callback. Before any coordinate is saved to the local database, it must pass three tests:

1. **The Accuracy Filter:** Raw GPS data comes with an accuracy radius. If the signal is weak and the accuracy is worse than 12 meters, the app completely ignores the point.
2. **The Minimum Displacement Filter:** To stop the "scribble" when standing still, the app compares the new location to the last known location. If the distance moved is less than 10 meters, it assumes this is just a GPS bounce and ignores it.
3. **The Teleportation Filter:** By dividing the distance by the time difference between updates, I calculate the speed in meters per second. If the speed is over 40 m/s (roughly 144 km/h), the app assumes the GPS had a massive error and drops the point.
</details>

<details>
<summary><b>💻 Implementation Code</b></summary>

Here is the tracking logic showing how the raw location data is filtered before interacting with the Room database on the IO thread:

```kotlin
override fun onLocationChanged(location: Location) {

    // Accuracy -> it is work on GPS Signles is it week or strong.
    if (location.accuracy > 12f){
        Log.d("TrackingService", "Ignored bad GPS: ${location.accuracy}m")
        return
    }

    // 2m ignore 4m ignore 5m ignore 6m accepted
    lastLocation?.let { last ->

        val distance = last.distanceTo(location) // distance between last and currenet location
        val timeDiffSeconds = (location.time - last.time) / 1000f // time difference between last location and new locatoin

        if (distance < 10f){  // if driver move little bit or less then 5 meters don't update the data
            Log.d("TrackingService","Ignored drift: $distance m")
            return
        }

        if(timeDiffSeconds > 0) {  // 5000 / 1000f = 5.0 seconds
            val speedPerSecond = distance / timeDiffSeconds  // 120 / 5.0 = 24.0 m/s (~86 km/h)
            if (speedPerSecond > 40f){
                Log.d("TrackingService","Ignored teleport drift. Speed $speedPerSecond")
                return
            }
        }
    }

    lastLocation = location

    val lat = location.latitude
    val lon = location.longitude

    serviceScope.launch {

        // Save the location
        val entity = LocationEntity(
            latitude = lat,
            longitude = lon
        )
        locationDao.insertLocation(entity)
    }

    Log.d("TrackingService", "NEW LOCATION $lat, $lon")
}

```
</details> 
<br>

---
# 🗺️ Engineering Log 2: Routing & Google Maps vs. OSRM

> When building a navigation app, you need a routing engine to calculate the path between coordinates. Most developers default to Google Maps, but moving to an open-source alternative like **OSRM (Open Source Routing Machine)** requires a fundamental shift in how you handle location data and network requests.
>Here is a breakdown of how OSRM works, how it differs from Google Maps, and how to implement the API in Android.

 
<details>
<summary><b>🌍 1. The Core Differences & The Coordinate Trap</b></summary>

<br>

While Google Maps and OSRM both provide turn-by-turn navigation, their underlying philosophies and data structures are completely different.

| Feature | Google Maps Directions API | OSRM (Open Source Routing Machine) |
| :--- | :--- | :--- |
| **Data Source** | Proprietary Google data | OpenStreetMap (OSM) data |
| **Cost** | Pay-per-API call (can get expensive) | 100% Free (especially if self-hosted) |
| **Coordinate System** | Geography-based (`Latitude, Longitude`) | Math/Cartesian-based (`Longitude, Latitude`) |
| **Response Format** | Custom JSON structure | Standard GeoJSON / Encoded Polylines |

### 🤔 Why the Difference? (No, they didn't do it just to annoy us)
It is easy to assume one of these engines just got it "wrong," but both choices are intentional engineering decisions based on who (or what) the system is built for.

**1. Why Google Maps uses Geography (`Lat, Lon`)**
Google Maps and Android are built for **humans**. For centuries, traditional navigation and cartography have written Latitude first, then Longitude. 
* **The Goal:** Usability. Google designed its consumer API to match how humans naturally read maps and globes.
* **Pros:** Highly intuitive for developers and users. Matches standard GPS hardware outputs.
* **Cons:** Under the hood, Google's servers have to burn extra processing power to flip these coordinates into math-friendly formats before running routing algorithms.

**2. Why OSRM uses Math (`Lon, Lat`)**
OSRM is built for **machines and extreme speed**. OSRM doesn't look at a globe; it flattens the world into a massive 2D mathematical graph (nodes and edges). 
* **The Goal:** Performance and Open Standards. In math, a graph requires an `(X, Y)` coordinate. Longitude goes East/West (X-axis) and Latitude goes North/South (Y-axis). Furthermore, OSRM strictly follows the open-source **GeoJSON standard**, which mandates the `[longitude, latitude]` format.
* **Pros:** Zero-cost math computations. By forcing the Android app to send data as `(X, Y)`, the OSRM server doesn't waste CPU cycles flipping coordinates and can inject the data directly into routing algorithms (like Dijkstra) for lightning-fast results.
* **Cons:** The "Developer Trap." It feels backward to mobile developers and easily causes critical routing bugs if forgotten.

### ⚠️ The Coordinate Trap: Routing to New Delhi
Because of this difference, failing to flip your coordinates is the #1 bug when integrating OSRM.

> **💡 Real-World Example:**
> The real-world coordinates for New Delhi are `Lat: 28.61, Lon: 77.20`. 
> If you take your Android location and blindly pass `28.61, 77.20` to OSRM, it reads it as `X: 28.61, Y: 77.20`. If you look that up on a map, it points to the **freezing Arctic Ocean near Norway!** You must always flip your Android location data to `Lon,Lat` before sending it to OSRM.

</details>

<details>
<summary><b>💻 2. Implementing the OSRM Network Call</b></summary>

<br>

To fetch a route from OSRM in Android, we use Retrofit. The OSRM API requires specific formatting for the URL path and several query parameters to get the exact data needed for drawing the "Blue Line" on a map.

Here is the Retrofit interface implementation:

```kotlin
interface OsrmApi {
    // We pass the coordinates as a string in format: "{lon1},{lat1};{lon2},{lat2}"
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        // 'encoded = true' prevents Retrofit from escaping characters like commas and semicolons
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full", 
        @Query("geometries") geometries: String = "geojson", 
        @Query("alternatives") alternatives: Boolean = true,
        @Query("steps") steps: Boolean = true
    ): OsrmResponse
} 
```
</details>

<details>
<summary><b> 🗺️ 3. Understanding OSRM's Coordinate String Format</b></summary>
<br>

When sending a routing request to the OSRM server, the coordinates must be passed in the URL as a single continuous string formatted like this: `{lon1},{lat1};{lon2},{lat2}`. 

Here is exactly how that structure works:

* **The Numbers (1 and 2):** These represent the sequence of your stops along the route. The **"1"** `(lon1, lat1)` represents your starting point (Origin), and the **"2"** `(lon2, lat2)` represents your ending point (Destination). 
* **The Comma ( `,` ):** The comma is used to bind an (X, Y) pair together. It pairs one specific Longitude (X) with its corresponding Latitude (Y) to pinpoint a single location on the map.
* **The Semicolon ( `;` ):** The semicolon acts as the connector *between* different locations. You can think of the semicolon as the word "TO". It tells OSRM, "Calculate a route from point 1 **TO** point 2." 

---

## 🚖 Why Do We Need Two Coordinate Pairs?

Because OSRM is a **routing engine**, not just a map marker. To draw a line (a route) on a map, you mathematically must have at least **two** points. If you only provide one point, the server knows where you *are*, but it has no idea where you want to *go*.

Here is how the API string breaks down in plain English:

* `lon1, lat1` = **Point A (Origin):** Where your route begins.
* `;` = **The Journey:** The semicolon translates to the word "TO". 
* `lon2, lat2` = **Point B (Destination):** Where your route ends.

If you only send a single coordinate pair, the OSRM server will reject the request because you haven't provided a finish line to calculate the path!
<br>

</details>

<details>
<summary><b>🧠 4. The Core Routing Logic: Step-by-Step</b></summary>

Regardless of the tech stack or programming language you are using, here is the exact logical flow you need to implement to successfully draw a navigation route on a map! 🗺️

<details>
<summary><b>📦 Step 1: Study the OSRM JSON Response Format (The Data Classes)</b></summary>

Before writing any routing logic, you must understand what OSRM is handing back to you. When you make a successful request, OSRM returns a massive JSON object. To make sense of it in code, we break it down into simple Data Classes. Think of it like a Russian nesting doll:

```kotlin
data class OsrmResponse(
    @SerializedName("routes")
    val routes: List<Route> // The outermost box: use this to access all route options
)

data class Route(
    val geometry : Geometry // The middle box: holds the shape of the line
)

data class Geometry(
    val coordinates : List<List<Double>>, // The core: [[75.7, 26.9], [75.8, 27.0]]
    val type : String // Tells us this is a "LineString"
)
```

* Look for the **routes** array (which contains all the alternative path options).
* Inside a specific route, look for the **geometry** object.
* Inside geometry, you will find **coordinates**. This is your golden ticket 🎟️: a massive 2D array formatted as `[[Longitude, Latitude], [Longitude, Latitude], ...]`.

Your goal is to unpack these data classes and extract this specific array so your map engine can draw a line connecting every single one of those dots.

</details>

<details>
<summary><b>📍 Step 2: Where exactly are we right now? (The Origin)</b></summary>

Before you can ask for a route, you need a starting point. Your app should query its local database or location cache to grab the absolute most recent GPS coordinate of the user. If the history is empty or the GPS signal is lost, halt the process and show a friendly "Waiting for GPS..." message to the user. 🚶‍♂️

</details>

<details>
<summary><b>🌐 Step 3: The Network Call</b></summary>

Take your Origin coordinate and your Destination coordinate, format them into the strict OSRM string requirement (`lon1,lat1;lon2,lat2`), and fire off your HTTP network request to the OSRM server. 🚀

</details>

<details>
<summary><b>✨ Step 4: The "Snap to Road" Trick (The Most Important Step)</b></summary>

If the API call is successful and you extract the coordinate array from your data classes, you must do one manual adjustment before drawing the line: **inject the user's raw starting coordinate at the very beginning (Index 0) of the array.**

**Why?** 🤔 
OSRM is a road router. It mathematically snaps your starting point to the absolute closest mapped road it can find. If your user is standing in the middle of a massive park or a large parking lot, drawing the raw OSRM route will leave an ugly visual gap on the screen between the user's blue GPS dot and where the route line actually begins. By manually inserting the user's exact current location to the start of the list, we forcefully connect the user's blue dot to the start of the OSRM road network, making the UI look completely seamless! 🎨

</details>

<details>
<summary><b>🛡️ Step 5: Graceful Error Handling</b></summary>

Mobile apps operate in unpredictable environments. Your routing function needs a safety net to catch these specific scenarios so the app doesn't crash:

* **📶 Network Failure:** The user lost internet. Return a friendly message instead of a crash.
* **🛑 Server Rejection (400 Errors):** The server rejected the request. This usually happens if the user tapped a destination that is impossible to drive to (like an ocean or an area with unmapped roads).
* **🧩 Bad Data Validation:** The server sent back a response, but it was corrupted or missing the geometry arrays. Catch this so your map drawing tool doesn't crash trying to read null data.
* **💥 The Catch-All:** A generic backup error handler at the very end of your logic, just in case something totally bizarre and unexpected happens!

</details>

</details>


