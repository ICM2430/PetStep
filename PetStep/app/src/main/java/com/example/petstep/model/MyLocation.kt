package com.example.petstep.model

import org.json.JSONObject
import java.util.Date

class MyLocation (val date : Date, val latitude: Double, val longitude: Double){
    fun toJSON() : JSONObject {
        val obj = JSONObject();
        obj.put("latitude", date)
        obj.put("longitude", latitude)
        obj.put("date", longitude)
        return obj
    }
}