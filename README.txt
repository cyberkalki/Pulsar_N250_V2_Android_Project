PULSAR N250 SMART DASHBOARD — VERSION 2.0
Android Studio source project. This ZIP is NOT a prebuilt APK.

BUILD:
1. Install Android Studio on a computer.
2. Extract this ZIP and open the Pulsar_N250_V2_Android_Project folder.
3. Allow Gradle sync; install Android SDK Platform 35 if prompted.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Transfer the generated APK to OPPO Reno 6 Pro and install.

FIRST RUN:
1. Open N250 Smart Dash 2.0.
2. Grant location permission; tap Start GPS.
3. Go to Settings inside the dashboard > Enable media notification access.
4. In Android's Notification access page, enable N250 Media Controls.
5. Pair your helmet intercom in Android Bluetooth settings.
6. Connect to internet for weather; weather uses Open-Meteo and GPS coordinates.
7. Swipe left/right to move between dashboard pages.

FEATURES:
- CarPlay-inspired landscape dashboard and swipe navigation.
- Animated N250 splash screen.
- GPS speed, accuracy, Trip A/B, ride timer, max/average speed.
- Ride history saved locally on the phone (up to 50 records).
- Open Google Maps, Spotify, YouTube Music, Waze and Google Assistant.
- Native battery percentage bridge.
- Weather lookup via Open-Meteo (internet required).
- Media play/pause, next and previous via active Android media session after notification-listener permission.
- Immersive fullscreen and screen stays awake while app is open.

LIMITATIONS / HONEST NOTES:
- This is NOT Apple CarPlay and does not replace Android with a new OS.
- Maps and music open in their own apps; embedded turn-by-turn map tiles and song metadata are not implemented.
- Android media controls depend on the media app exposing an active MediaSession and notification access being enabled.
- GPS speed and distance are estimates, not certified bike telemetry.
- Trip A/B are app-side distance counters; Trip B persists locally. Ride history is stored locally.
- Automatic launch on device boot is not forced: Android/ColorOS restrict background activity launches. Open the app manually or configure an allowed auto-start option in ColorOS if available.
- Voice assistant invokes Android's configured voice command handler; availability depends on Google Assistant and device settings.
- No engine RPM, fuel level, gear, or ECU data without compatible external hardware.
- Do not interact while riding. Mount securely with vibration damping, keep original cluster visible, and protect from water/heat.
