package com.cyberkalki.n250v2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.content.res.Configuration
import android.webkit.*
import java.lang.ref.WeakReference
import org.json.JSONObject
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    companion object {
        @Volatile private var current = WeakReference<MainActivity>(null)
        fun sendJavascript(script: String) {
            val activity = current.get() ?: return
            activity.runOnUiThread { if (activity.webReady) activity.web.evaluateJavascript(script, null) }
        }
    }
    private lateinit var web: WebView
    private var webReady=false
    private val locationRequest = 42
    private var pendingOrigin: String? = null
    private var pendingCallback: GeolocationPermissions.Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        current = WeakReference(this)
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()
        web = WebView(this)
        web.clearCache(true)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = true
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    openExternal(url); return true
                }
                return false
            }
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                if (origin == null || callback == null) return
                if (hasLocationPermission()) callback.invoke(origin, true, false)
                else {
                    pendingOrigin = origin; pendingCallback = callback
                    ActivityCompat.requestPermissions(this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), locationRequest)
                }
            }
        }
        web.addJavascriptInterface(Bridge(this), "N250")
        setContentView(web)
        startService(Intent(this, PhoneSensors::class.java))
        publishDisplayInfo()
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                if (request.isForMainFrame && (uri.scheme == "http" || uri.scheme == "https")) {
                    openExternal(uri.toString())
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                webReady = true
                publishDisplayInfo()
            }
        }
        web.loadUrl("file:///android_asset/index.html?v=2.1")
    }

    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); publishDisplayInfo() }
    private fun publishDisplayInfo() {
        if (!::web.isInitialized || !webReady) return
        val rotation = if (android.os.Build.VERSION.SDK_INT >= 30) display?.rotation ?: 0 else @Suppress("DEPRECATION") windowManager.defaultDisplay.rotation
        var edge = "none"
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            val rects = window.decorView.rootWindowInsets?.displayCutout?.boundingRects.orEmpty()
            if (rects.isNotEmpty()) {
                val r = rects.maxByOrNull { it.width() * it.height() }!!
                val w = window.decorView.width.coerceAtLeast(1); val h = window.decorView.height.coerceAtLeast(1)
                edge = if (r.width() >= r.height()) { if (r.top < h / 2) "top" else "bottom" } else { if (r.left < w / 2) "left" else "right" }
                if (rotation == 2) edge = when(edge) { "left" -> "right"; "right" -> "left"; "top" -> "bottom"; "bottom" -> "top"; else -> edge }
            }
        }
        web.post { web.evaluateJavascript("window.setDisplayInfo('$edge',$rotation)", null) }
    }
    private fun applyOrientation(value: String) {
        requestedOrientation = when(value) { "landscape" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE; "reverse" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE; else -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationRequest) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            pendingCallback?.invoke(pendingOrigin, granted, false)
            pendingCallback = null
            pendingOrigin = null
            if (granted) startRideTracking() else sendJavascript("window.onLocationPermission(false)")
        }
    }

    private fun startRideTracking() {
        val repository = RideRepository(this)
        val ride = repository.active() ?: repository.start(System.currentTimeMillis())
        val serviceIntent = Intent(this, RideLocationService::class.java).putExtra("rideId", ride.optString("id"))
        ContextCompat.startForegroundService(this, serviceIntent)
        sendJavascript("window.onNativeState('active',${JSONObject.quote(ride.optString("id"))},${ride.optLong("start")})")
    }

    private fun openExternal(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (_: Exception) {}
    }

    private fun hideSystemUi() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) hideSystemUi() }
    override fun onPause() { if (::web.isInitialized) web.onPause(); super.onPause() }
    override fun onResume() { super.onResume(); if (::web.isInitialized) web.onResume() }
    override fun onDestroy(){
        if(current.get()===this) current.clear()
        if(::web.isInitialized){web.stopLoading();web.removeJavascriptInterface("N250");web.destroy()}
        stopService(Intent(this, PhoneSensors::class.java))
        super.onDestroy()
    }
    @Deprecated("Deprecated in Android")
    override fun onBackPressed() { if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed() }

    inner class Bridge(private val activity: Activity) {
        @JavascriptInterface fun startRideGps() {
            activity.runOnUiThread {
                if (!hasLocationPermission()) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        locationRequest
                    )
                    return@runOnUiThread
                }
                startRideTracking()
            }
        }
        @JavascriptInterface fun resumeRide(){ val r=RideRepository(activity).active()?:return; if(hasLocationPermission()) ContextCompat.startForegroundService(activity,Intent(activity,RideLocationService::class.java).putExtra("rideId",r.optString("id"))) }
        @JavascriptInterface fun finishRide(id: String) {
            if (RideRepository(activity).finish(id)) {
                activity.stopService(Intent(activity, RideLocationService::class.java))
                sendJavascript("window.onNativeState('stopped','')")
                sendJavascript("window.onRides(${JSONObject.quote(RideRepository(activity).list())})")
            }
        }
        @JavascriptInterface fun loadRides(){ sendJavascript("window.onRides(${JSONObject.quote(RideRepository(activity).list())})") }
        @JavascriptInterface fun navigateTo(destination: String) {
            activity.runOnUiThread {
                val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination)}")
                launch(Intent(Intent.ACTION_VIEW, uri))
            }
        }
        @JavascriptInterface fun open(action: String) { activity.runOnUiThread {
            when(action) {
                "maps" -> launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com")).setPackage("com.google.android.apps.maps"))
                "spotify" -> launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com")).setPackage("com.spotify.music"))
                "ytmusic" -> launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com")).setPackage("com.google.android.apps.youtube.music"))
                "waze" -> launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul")).setPackage("com.waze"))
                "assistant" -> launch(Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "bluetooth" -> launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                "phone" -> launch(Intent(Intent.ACTION_DIAL))
                "settings" -> launch(Intent(Settings.ACTION_SETTINGS))
                "notificationAccess" -> launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                "batterySettings" -> launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        } }
        @JavascriptInterface fun fullscreen() { activity.runOnUiThread { hideSystemUi() } }
        @JavascriptInterface fun preference(key: String, value: String?): String {
            val prefs = activity.getSharedPreferences("rider_ui", Context.MODE_PRIVATE)
            if (value != null) prefs.edit().putString(key, value).apply()
            return prefs.getString(key, when(key) { "dock" -> "auto"; "orientation" -> "auto"; else -> "" }) ?: ""
        }
        @JavascriptInterface fun setOrientation(value: String) { activity.runOnUiThread { applyOrientation(value) } }
        @JavascriptInterface fun haptic() { activity.runOnUiThread { if (android.os.Build.VERSION.SDK_INT >= 29) web.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM) else web.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY) } }
        @JavascriptInterface fun getBattery(): Int {
            val bm = activity.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0,100)
        }
        @JavascriptInterface fun getPhoneTemperature(): Double {
            val battery = activity.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            return (battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
        }
        @JavascriptInterface fun calibrateLean() {
            activity.startService(Intent(activity, PhoneSensors::class.java).setAction("calibrate_lean"))
        }

        @JavascriptInterface fun sendCrashAlert(latitude: Double, longitude: Double) {
            val phone = activity.getSharedPreferences("rider_ui", 0).getString("emergency", "").orEmpty()
            if (phone.isBlank()) return
            activity.runOnUiThread {
                val location = "https://maps.google.com/?q=$latitude,$longitude"
                val sms = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
                    .putExtra("sms_body", "Rider safety alert. Please check on me. Last known location: $location")
                try { activity.startActivity(sms) } catch (_: Exception) { }
            }
        }
        @JavascriptInterface fun media(action: String) {
            activity.runOnUiThread {
                val service = MediaListener.instance
                if (service == null) {
                    launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    sendJavascript("window.onMediaUnavailable()")
                    return@runOnUiThread
                }
                service.transport(action)
            }
        }
    }
    private fun launch(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            val uri = intent.data ?: return
            try { startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) { }
        }
    }
}
