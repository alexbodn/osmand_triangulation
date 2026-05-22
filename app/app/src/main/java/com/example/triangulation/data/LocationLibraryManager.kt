package com.example.triangulation.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LocationLibraryManager(private val context: Context) {

    private val file = File(context.filesDir, "locations.json")

    fun getLocations(): List<LibraryLocation> {
        if (!file.exists()) {
            return emptyList()
        }
        val list = mutableListOf<LibraryLocation>()
        try {
            val content = file.readText()
            val array = JSONArray(content)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    LibraryLocation(
                        lat = obj.getDouble("lat"),
                        lon = obj.getDouble("lon"),
                        desc = if (obj.has("desc") && !obj.isNull("desc")) obj.getString("desc") else null,
                        img = if (obj.has("img") && !obj.isNull("img")) obj.getString("img") else null
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveLocations(locations: List<LibraryLocation>) {
        try {
            val array = JSONArray()
            for (loc in locations) {
                val obj = JSONObject()
                obj.put("lat", loc.lat)
                obj.put("lon", loc.lon)
                obj.put("desc", loc.desc)
                obj.put("img", loc.img)
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addLocation(location: LibraryLocation) {
        val current = getLocations().toMutableList()
        // Replace if exists with same lat/lon (with minor tolerance)
        val index = current.indexOfFirst { Math.abs(it.lat - location.lat) < 0.00001 && Math.abs(it.lon - location.lon) < 0.00001 }
        if (index != -1) {
            current[index] = location
        } else {
            current.add(location)
        }
        saveLocations(current)
    }

    fun removeLocation(lat: Double, lon: Double) {
        val current = getLocations().toMutableList()
        val index = current.indexOfFirst { Math.abs(it.lat - lat) < 0.00001 && Math.abs(it.lon - lon) < 0.00001 }
        if (index != -1) {
            current.removeAt(index)
            saveLocations(current)
        }
    }

    fun isLocationInLibrary(lat: Double, lon: Double): LibraryLocation? {
        val current = getLocations()
        return current.firstOrNull { Math.abs(it.lat - lat) < 0.00001 && Math.abs(it.lon - lon) < 0.00001 }
    }
}
