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
        // Replace if exists with exact same lat/lon AND desc
        val index = current.indexOfFirst { it.lat == location.lat && it.lon == location.lon && it.desc == location.desc }
        if (index != -1) {
            current[index] = location
        } else {
            current.add(location)
        }
        saveLocations(current)
    }

    fun removeLocation(lat: Double, lon: Double, desc: String? = null) {
        val current = getLocations().toMutableList()
        val index = if (desc != null) {
            current.indexOfFirst { it.lat == lat && it.lon == lon && it.desc == desc }
        } else {
            current.indexOfFirst { it.lat == lat && it.lon == lon }
        }

        if (index != -1) {
            current.removeAt(index)
            saveLocations(current)
        }
    }

fun isLocationInLibrary(lat: Double, lon: Double, desc: String? = null): LibraryLocation? {
        val current = getLocations()
        return if (desc != null) {
            current.firstOrNull { it.lat == lat && it.lon == lon && it.desc == desc }
        } else {
            // When querying just by coordinates (e.g. from Home tab active locations),
            // return the first match at that exact spot if one exists.
            current.firstOrNull { it.lat == lat && it.lon == lon }
        }
    }
}
