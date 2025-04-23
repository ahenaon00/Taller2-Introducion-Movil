package com.example.taller2

import org.json.JSONObject
import java.util.Date

class LocationClass (val date : Date, val latitude: Double, val longitude: Double){
    fun toJSON() : JSONObject {
        val obj = JSONObject();
        obj.put("latitude", latitude)  // Corrected
        obj.put("longitude", longitude)  // Corrected
        obj.put("date", date.time)
        return obj
    }
}