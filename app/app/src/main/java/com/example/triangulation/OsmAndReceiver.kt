package com.example.triangulation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class OsmAndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val lat = intent.getDoubleExtra("lat", Double.NaN)
        val lon = intent.getDoubleExtra("lon", Double.NaN)

        if (!lat.isNaN() && !lon.isNaN()) {
            val mainIntent = Intent(context, MainActivity::class.java)
            mainIntent.putExtra("lat", lat)
            mainIntent.putExtra("lon", lon)
            mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            context.startActivity(mainIntent)
        }
    }
}
