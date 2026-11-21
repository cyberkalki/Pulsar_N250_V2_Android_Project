package com.cyberkalki.n250v2

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import androidx.core.app.NotificationCompat

class RideLocationService: Service(), LocationListener {
    private lateinit var repo: RideRepository
    private lateinit var manager: LocationManager
    private var lastAccepted: Location?=null
    override fun onCreate(){super.onCreate();repo=RideRepository(this);manager=getSystemService(LOCATION_SERVICE) as LocationManager;createChannel()}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int { val activeRide=repo.active(); val id=intent?.getStringExtra("rideId")?:activeRide?.optString("id"); if(id.isNullOrBlank()){stopSelf(startId);return START_NOT_STICKY}; val n=notification(); if(Build.VERSION.SDK_INT>=29)startForeground(11,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION) else startForeground(11,n); if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED||checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)==android.content.pm.PackageManager.PERMISSION_GRANTED){try{manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,0f,this,Looper.getMainLooper())}catch(_:Exception){}}; sendState("active",id); return START_NOT_STICKY }
    override fun onLocationChanged(loc:Location){if(loc.accuracy>35)return; val prior=lastAccepted; if(prior!=null&&loc.time-prior.time>0){ val derived=prior.distanceTo(loc)/(loc.time-prior.time)*1000f; if(loc.speed<0.2f&&derived<0.5f)return }; lastAccepted=loc; val sp=if(loc.hasSpeed())loc.speed.toDouble() else 0.0; val r=repo.addFix(loc.latitude,loc.longitude,sp,loc.time)?:return; val js="window.onNativeFix(${loc.latitude},${loc.longitude},${sp*3.6},${loc.accuracy},${loc.time})"; MainActivity.sendJavascript(js); val b=Intent("com.cyberkalki.n250.RIDE_STATE").putExtra("state","active").putExtra("id",r.optString("id")); sendBroadcast(b) }
    override fun onBind(intent:Intent?)=null
    override fun onDestroy(){manager.removeUpdates(this);super.onDestroy()}
    private fun sendState(s:String,id:String){MainActivity.sendJavascript("window.onNativeState('$s',${org.json.JSONObject.quote(id)},${repo.active()?.optLong("start") ?: 0L})")}
    private fun notification():Notification { val i=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT);return NotificationCompat.Builder(this,"ride_tracking").setSmallIcon(android.R.drawable.ic_menu_compass).setContentTitle("Rider ride recording").setContentText("GPS ride tracking is active").setContentIntent(i).setOngoing(true).build() }
    private fun createChannel(){if(Build.VERSION.SDK_INT>=26){val c=NotificationChannel("ride_tracking","Ride tracking",NotificationManager.IMPORTANCE_LOW);(getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(c)}}
}
