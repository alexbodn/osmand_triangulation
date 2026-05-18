# Triangulation App for OsmAnd

[![Android CI](https://github.com/AlexBodn/Triangulation/actions/workflows/android.yml/badge.svg)](https://github.com/AlexBodn/Triangulation/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Triangulation** is a lightweight Android utility designed to integrate seamlessly with the [OsmAnd](https://osmand.net/) map application. It allows users to perform precise geolocation triangulation directly from the field and instantly visualizes the resulting intersection points and lines as GPX overlays within the OsmAnd interface.

## Features

* **OsmAnd AIDL Integration:** Uses OsmAnd's official V2 AIDL API to silently import GPX track data without triggering annoying dialog prompts, providing a smooth, uninterrupted user experience.
* **Accurate Triangulation:** Calculates geographic intersections based on user-provided coordinates and azimuths.
* **Native Magnetic Declination:** Utilizes Android's hardware `GeomagneticField` to automatically calculate and apply magnetic declination for accurate compass-to-true-north conversions—no external databases required.
* **Instant Visualization:** Automatically switches focus back to OsmAnd, panning the map directly to the calculated waypoint.
* **Privacy Focused:** Operates without tracking your location in the background. It only processes explicit location data shared by the user.

## Screenshots

*To embed an image in this README, you can either:*
1. *Upload an image file (e.g., `screenshot.png`) into a directory in your repository like `images/` and link to it like this:*
   `![App Screenshot](images/screenshot.png)`
2. *Or simply drag and drop the image file directly into the GitHub web editor while editing this file, and GitHub will automatically generate the embed link for you!*

<!-- Remove the text above and place your image embed link here! -->

## Prerequisites

To use this application, you must have OsmAnd installed on your device.

1. Install [OsmAnd](https://play.google.com/store/apps/details?id=net.osmand) or [OsmAnd+](https://play.google.com/store/apps/details?id=net.osmand.plus) from the Google Play Store.
2. Ensure you enable the Triangulation app within OsmAnd's **Plugin Menu** so the two applications can communicate securely. (Note: The "OsmAnd development" plugin is not required).

## Installation

You do not need to build the app from source to use it.

1. Go to the [Releases page](../../releases/latest).
2. Download the `triangulation.apk` file attached to the latest release.
3. Open the downloaded APK on your Android device to install it. (You may need to allow "Install unknown apps" from your browser or file manager).

## Building from Source

If you wish to modify the application or build it yourself:

1. Clone the repository:
   ```bash
   git clone https://github.com/AlexBodn/Triangulation.git
   ```
2. Navigate to the project directory:
   ```bash
   cd Triangulation/app
   ```
3. Build the debug APK using the Gradle wrapper:
   ```bash
   ./gradlew assembleDebug
   ```
   *The built APK will be located in `app/build/outputs/apk/debug/`.*

## Open Source & Attributions

This project uses open-source software, including AndroidX libraries and Material Components.

* The application code is released under the **MIT License**. See the [LICENSE](LICENSE) file for details.
* For a full list of third-party attributions and their respective licenses, please see the [ATTRIBUTIONS.md](ATTRIBUTIONS.md) file.
