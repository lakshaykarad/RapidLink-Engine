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
