# AR Measure Area 📐📱

An advanced Augmented Reality (AR) Android application built with **Jetpack Compose** and **Sceneview (ARCore)** that allows users to measure real-world distances, calculate enclosed surface areas, and measure vertical dimensions using their smartphone camera.

---

## 🌟 Key Features

1. **AR Surface & Plane Detection**
   - Utilizes Google ARCore and Sceneview to detect horizontal and vertical surfaces in real time.
   - Intelligent tracking failure notifications guide users when lighting or surface textures are insufficient.

2. **Linear Distance Measurement**
   - Tap to place 3D marker nodes in AR space.
   - Automatically computes and displays real-time cumulative distances between points in centimeters (cm).

3. **Enclosed Surface Area Calculation**
   - Automatically calculates the polygon area enclosed by placed markers using the **Convex Hull** algorithm and the **Shoelace Formula**.
   - Displays real-time area measurements in square centimeters (`cm²`).

4. **Vertical & Cube Dimension Measurement**
   - Supports vertical height and 3D cube measuring modes using camera pitch and depth calculations relative to anchor points.

5. **Interactive UI & Coaching Overlays**
   - Real-time instructional guidance (`InstructionalOverlay`) instructing users to point the camera down or add points.
   - Smooth animations and Material 3 design system components.

---

## 🛠️ Tech Stack & Architecture

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3.
- **AR Engine**: [Sceneview AR](https://github.com/Sceneview/sceneview-android) (Kotlin ARCore wrapper).
- **Language**: Kotlin 2.1.20.
- **Build System**: Gradle 8.12 with Android Gradle Plugin (AGP) 8.6+.
- **Core Components**:
  - [`DistanceScreen.kt`](file:///Users/mauliktogadiya/Maulik/Android/MI/ar-measure-area/app/src/main/java/com/hanif/armeasurearea/DistanceScreen.kt): Main AR session and gesture handling screen.
  - [`AreaDistanceHelper.kt`](file:///Users/mauliktogadiya/Maulik/Android/MI/ar-measure-area/app/src/main/java/com/hanif/armeasurearea/utils/AreaDistanceHelper.kt): Geometry utilities for total distance calculation, convex hull computation, and Shoelace area formula.
  - [`NodeHelper.kt`](file:///Users/mauliktogadiya/Maulik/Android/MI/ar-measure-area/app/src/main/java/com/hanif/armeasurearea/utils/NodeHelper.kt): AR anchor and line transformation management.
  - [`InstructionalOverlay.kt`](file:///Users/mauliktogadiya/Maulik/Android/MI/ar-measure-area/app/src/main/java/com/hanif/armeasurearea/composables/InstructionalOverlay.kt): Dynamic instructional card overlay based on tracking state.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Koala or newer.
- Android device or emulator supporting **Google Play Services for AR (ARCore)** (API level 24+).
- Gradle 8.12 (configured via Gradle Wrapper).

### Building and Running
1. Clone or open the project in Android Studio.
2. Sync project with Gradle files.
3. Connect an ARCore-compatible physical device with USB debugging enabled.
4. Run the app (`app:assembleDebug`).

---

## 📄 License
This project is open-source and available under the MIT License.
