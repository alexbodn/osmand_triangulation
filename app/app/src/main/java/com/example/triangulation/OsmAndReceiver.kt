package com.example.triangulation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class OsmAndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val lat = intent.getDoubleExtra("LAT", Double.NaN)
            val lon = intent.getDoubleExtra("LON", Double.NaN)
            val pointId = intent.getStringExtra("POINT_ID")

            Log.d("Triangulation", "OsmAndReceiver received intent: ${intent.action}. lat=$lat, lon=$lon, pointId=$pointId")

            // If there's no actionable data, we should not disrupt the user by popping open the app
            if (lat.isNaN() && lon.isNaN() && pointId.isNullOrEmpty()) {
                 Log.d("Triangulation", "Ignoring broadcast - no actionable location data found.")
                 return
            }

            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

                if (!lat.isNaN() && !lon.isNaN()) {
                    putExtra("lat", lat)
                    putExtra("lon", lon)
                } else if (pointId != null && pointId.contains(",")) {
                    try {
                        val parts = pointId.split(",")
                        putExtra("lat", parts[0].toDouble())
                        putExtra("lon", parts[1].toDouble())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                     putExtra("pointId", pointId)
                }
            }

            context.startActivity(mainIntent)
        } catch (e: Exception) {
            Log.e("Triangulation", "Error in OsmAndReceiver", e)
        }
    }
}
