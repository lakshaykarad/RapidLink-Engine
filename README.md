# RapidLink - Offline-First Navigation Engine

> A native Android navigation system built without the Google Maps SDK. 
> Features resilient background tracking, offline path storage, and hybrid routing logic.

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple) ![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-blue) ![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-green) ![Status](https://img.shields.io/badge/Status-Offline%20First-orange)

## 💡 The Engineering Challenge
Most navigation apps rely heavily on the Google Maps SDK and constant internet connectivity. My goal was to build a **resilient, cost-effective alternative** that maintains data integrity even in "dead zones."

**RapidLink** separates *Tracking* from *Routing*:
* **Tracking (Offline):** Uses a local Room Database and Foreground Services to capture the user's path ("Red Trail") 100% of the time, regardless of network status.
* **Routing (Hybrid):** Fetches navigation paths ("Blue Line") via OSRM but caches vector tiles locally for smooth rendering.


## 🗺️ RapidLink — *Screen Shorts*
<div align="center">
<table>
  <tr>
    <td align="center">
      <img src="app/src/main/res/drawable/Map%20View" width="220"/>
      <br/><sub><b>Map View</b></sub>
    </td>
    <td align="center">
      <img src="app/src/main/res/drawable/Searching" width="220"/>
      <br/><sub><b>Location Search</b></sub>
    </td>
    <td align="center">
      <img src="app/src/main/res/drawable/Route" width="220"/>
      <br/><sub><b>Dropped Pin</b></sub>
    </td>
    <td align="center">
      <img src="app/src/main/res/drawable/Multiple%20Route" width="220"/>
      <br/><sub><b>Multiple Routes</b></sub>
    </td>
    <td align="center">
      <img src="app/src/main/res/drawable/Location%20History" width="220"/>
      <br/><sub><b>Location History</b></sub>
    </td>
  </tr>
</table>
</div>


## 📱 Key Features

### 1. 🛡️ Resilient Background Tracking
* Implemented a **Foreground Service** with persistent Notification Channels.
* Ensures GPS tracking continues even when the app is killed or the screen is locked (Android 14 compatible).

### 2. 🔌 Offline-First Architecture
* **Single Source of Truth:** The UI never queries the network directly. It observes the **Room Database**.
* **Data Persistence:** GPS coordinates are written to SQLite every 2 seconds via a non-blocking IO thread.
* *Result:* Zero data loss during network failures.

### 3. 🗺️ Custom Mapping Stack (0% Google Dependency)
* **Rendering:** MapLibre Native SDK (Vector Tiles).
* **Geocoding:** Nominatim API (City Search).
* **Routing:** OSRM API (Turn-by-turn Navigation).
* *Benefit:* Unlimited scalability with zero API billing costs.

---

## 🛠️ Tech Stack

| Category | Technology Used |
| :--- | :--- |
| **Language** | Kotlin (100%) |
| **UI Toolkit** | Jetpack Compose (Material3) |
| **DI** | Dagger Hilt |
| **Async** | Coroutines & Flow |
| **Local DB** | Room Database (SQLite) |
| **Networking** | Retrofit2 & OkHttp |
| **Maps** | MapLibre SDK + MapTiler |
| **APIs** | OSRM (Routing), Nominatim (Search) |

---

## 🏗️ Architecture (MVVM + Clean)

The app follows a strict **Unidirectional Data Flow (UDF)**:

1.  **Service Layer:** The `TrackingService` runs in the background, collecting raw hardware data.
2.  **Repository:** Acts as the data mediator. It writes new points to **Room** on an `IO` thread.
3.  **ViewModel:** Exposes the data as a `StateFlow`.
4.  **UI (Compose):** Reactively consumes the Flow. It simply "draws what is in the database."

---

# RapidLink Version 2.0 
> 🚧 UPDATE: Version 2.0 is currently in active development! 🚧
> Notice: The code in this public repository reflects V1.0. I am currently building V2.0 in a private repository to overhaul the architecture.

> 🧠 **Engineering Deep Dive:** > Curious about how I am handling errors without Google Services, or how the offline tracking engine works? 
> **[👉 Click here to read the V2.0 Engineering Details & Problem-Solving Log](./DETAILS.md)**

