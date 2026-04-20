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

# 🗺️ Engineering Log 2: Routing & Google Maps vs. OSRM

> When building a navigation app, you need a routing engine to calculate the path between coordinates. Most developers default to Google Maps, but moving to an open-source alternative like **OSRM (Open Source Routing Machine)** requires a fundamental shift in how you handle location data and network requests.
> Here is a breakdown of how OSRM works, how it differs from Google Maps, and how to implement the API in Android.

---

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

---

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

---

<details>
<summary><b>🗺️ 3. Understanding OSRM's Coordinate String Format</b></summary>

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

</details>

---

<details>
<summary><b>🧠 4. The Core Routing Logic: Step-by-Step</b></summary>

<br>

> No matter what tech stack or language you're using, this is the exact flow I followed to draw a navigation route on a map. I'll break it down into simple steps so it's easy to understand! 🗺️

---

<details>
<summary><b>📦 Step 1: Study the OSRM JSON Response Format (The Data Classes)</b></summary>

<br>

Before writing any routing logic, I had to first understand what data OSRM sends back. When the API call is successful, OSRM returns a big JSON object. To work with it properly in code, I broke it down into simple **Data Classes** — think of it like opening a set of nested boxes, one inside the other.

```kotlin
data class OsrmResponse(
    @SerializedName("routes")
    val routes: List<Route> // Outermost box — holds all the route options
)

data class Route(
    val geometry: Geometry // Middle box — holds the shape/path of the route
)

data class Geometry(
    val coordinates: List<List<Double>>, // The actual path — [[lon, lat], [lon, lat], ...]
    val type: String                     // Will be "LineString"
)
```

**What to look for:**
- Find the `routes` array — it contains all possible path options returned by the server.
- Inside a route, find the `geometry` object.
- Inside geometry, find `coordinates` — this is the **golden ticket** 🎟️.

The `coordinates` field is a 2D array in the format `[[Longitude, Latitude], [Longitude, Latitude], ...]`. My goal was to extract this array and hand it to the map engine so it could draw a connected line through every single point.

</details>

---

<details>
<summary><b>📍 Step 2: Where Exactly Are We Right Now? (The Origin)</b></summary>

<br>

Before asking for a route, I need a **starting point**. The app should query its local database or location cache to grab the most recent GPS coordinate of the user.

If the location history is empty or the GPS signal is lost, I stop the process here and show a friendly message like:

> _"Waiting for GPS signal... 🚶‍♂️"_

Never try to request a route without a valid origin — it will just fail or give wrong results.

</details>

---

<details>
<summary><b>🌐 Step 3: The Network Call</b></summary>

<br>

Once I have both the **origin** and **destination** coordinates, I format them into the OSRM URL format:

```
lon1,lat1;lon2,lat2
```

Then I fire off the HTTP GET request to the OSRM server. 🚀

Simple as that — just make sure the coordinates are in **longitude, latitude** order (not the usual lat, lon — this tripped me up at first!).

</details>

---

<details>
<summary><b>✨ Step 4: The "Snap to Road" Trick (The Most Important Step!)</b></summary>

<br>

Once the API responds and I extract the coordinates array, I do **one manual fix** before drawing the route on the map:

> **Inject the user's actual GPS location at Index 0 (the very start) of the coordinates array.**

### Why does this matter? 🤔

OSRM is a road router — it snaps your starting point to the nearest road it can find. If the user is standing in the middle of a large park or a parking lot, the OSRM route won't start exactly at the user's blue dot. This creates an **ugly visual gap** on the screen between where the user is and where the route line begins.

By manually inserting the user's raw GPS location at the beginning of the list, I connect the user's dot directly to the start of the route — making everything look **seamless and clean**! 🎨

</details>

---

<details>
<summary><b>🛡️ Step 5: Graceful Error Handling</b></summary>

<br>

Mobile apps run in unpredictable environments, so I made sure to handle these specific failure cases so the app never crashes:

| Scenario | What It Means | How I Handle It |
|---|---|---|
| 📶 **Network Failure** | User lost internet mid-request | Show a friendly "No internet" message instead of crashing |
| 🛑 **Server Rejection (400 errors)** | Destination is unreachable (ocean, unmapped area) | Show an error like "Route not found for this destination" |
| 🧩 **Bad / Missing Data** | Server responded but geometry is null or corrupted | Validate before drawing — don't pass null to the map engine |
| 💥 **Catch-All / Unknown Error** | Something totally unexpected happened | A generic fallback handler at the very end, just in case |

Always wrap your routing logic in proper try-catch blocks and test each of these cases manually before shipping! 🔒

</details>

</details>
