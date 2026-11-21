package com.cyberkalki.n250v2

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.*

class RideRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ride_store", Context.MODE_PRIVATE)
    @Synchronized fun active(): JSONObject? = prefs.getString("active", null)?.let { runCatching { JSONObject(it) }.getOrNull() }
    @Synchronized fun start(now: Long): JSONObject { val ride = JSONObject().put("id", UUID.randomUUID().toString()).put("start", now).put("distance", 0.0).put("moving", 0L).put("maxSpeed", 0.0).put("avgSpeed", 0.0).put("speedSum", 0.0).put("speedCount", 0).put("lastLat", JSONObject.NULL).put("lastLon", JSONObject.NULL); prefs.edit().putString("active", ride.toString()).apply(); return ride }
    @Synchronized fun addFix(lat: Double, lon: Double, speed: Double, time: Long): JSONObject? { val r=active()?:return null; val oldLat=r.optDouble("lastLat",Double.NaN); val oldLon=r.optDouble("lastLon",Double.NaN); if(oldLat.isFinite()&&oldLon.isFinite()){ val d=distance(oldLat,oldLon,lat,lon); if(d in 2.0..150.0) r.put("distance",r.optDouble("distance")+d) }; val prev=r.optLong("lastTime",time); if(speed>0.56){ r.put("moving",r.optLong("moving")+((time-prev).coerceIn(0,5000))); r.put("speedSum",r.optDouble("speedSum")+speed); r.put("speedCount",r.optInt("speedCount")+1) }; r.put("avgSpeed",if(r.optInt("speedCount")>0)r.optDouble("speedSum")/r.optInt("speedCount") else 0.0).put("maxSpeed",max(r.optDouble("maxSpeed"),speed)).put("lastLat",lat).put("lastLon",lon).put("lastTime",time); prefs.edit().putString("active",r.toString()).apply(); return r }
    @Synchronized fun finish(id:String): Boolean { val r=active()?:return false; if(r.optString("id")!=id)return false; r.put("end",System.currentTimeMillis()); val all=JSONArray(prefs.getString("rides","[]")); all.put(0,r); while(all.length()>100)all.remove(all.length()-1); prefs.edit().putString("rides",all.toString()).remove("active").apply(); return true }
    fun list(): String = prefs.getString("rides","[]") ?: "[]"
    companion object { fun distance(a:Double,b:Double,c:Double,d:Double):Double { val p=Math.PI/180; val x=(c-a)*p; val y=(d-b)*p; val h=sin(x/2).pow(2)+cos(a*p)*cos(c*p)*sin(y/2).pow(2); return 6371000*2*asin(sqrt(h.coerceIn(0.0,1.0))) } }
}
