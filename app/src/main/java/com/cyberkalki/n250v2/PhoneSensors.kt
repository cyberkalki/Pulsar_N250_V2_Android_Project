package com.cyberkalki.n250v2

import android.app.*
import android.content.*
import android.hardware.*
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.content.IntentFilter

class PhoneSensors: Service(), SensorEventListener {
 private lateinit var sm:SensorManager; private var light:Sensor?=null; private var accel:Sensor?=null; private var gyro:Sensor?=null; private var lightValue=100f; private var baseline=0.0; private var calibrating=false; private var lastEvent=0L
 override fun onCreate(){super.onCreate();baseline=getSharedPreferences("rider_ui",0).getFloat("leanBaseline",0f).toDouble();sm=getSystemService(SENSOR_SERVICE) as SensorManager;light=sm.getDefaultSensor(Sensor.TYPE_LIGHT);accel=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);gyro=sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE); light?.let{sm.registerListener(this,it,SensorManager.SENSOR_DELAY_NORMAL)};accel?.let{sm.registerListener(this,it,SensorManager.SENSOR_DELAY_GAME)} }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int { if(intent?.action=="calibrate_lean")calibrating=true;return START_STICKY }
 override fun onSensorChanged(e:SensorEvent){when(e.sensor.type){Sensor.TYPE_LIGHT->{lightValue=e.values[0];MainActivity.sendJavascript("window.onAmbientLight(${lightValue})")};Sensor.TYPE_ACCELEROMETER->{val angle=Math.toDegrees(kotlin.math.atan2(e.values[0].toDouble(),e.values[2].toDouble()));if(calibrating){baseline=angle;calibrating=false;getSharedPreferences("rider_ui",0).edit().putFloat("leanBaseline",baseline.toFloat()).apply()};val lean=angle-baseline;MainActivity.sendJavascript("window.onLean(${lean})");val mag=kotlin.math.sqrt(e.values.sumOf{(it*it).toDouble()});if(mag>24&&System.currentTimeMillis()-lastEvent>5000){lastEvent=System.currentTimeMillis();MainActivity.sendJavascript("window.onRideEvent('impact',${mag})")}}} }
 override fun onAccuracyChanged(s:Sensor?,a:Int){}
 override fun onBind(i:Intent?):IBinder?=null
 override fun onDestroy(){sm.unregisterListener(this);super.onDestroy()}
}
