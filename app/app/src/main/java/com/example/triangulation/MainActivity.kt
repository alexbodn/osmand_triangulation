package com.example.triangulation

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null

    private lateinit var ivArrow: ImageView
    private lateinit var etAzimuth: EditText
    private lateinit var tvBackAzimuth: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnReset: Button

    private var currentAzimuth = 0f
    private var selectedLocations = mutableListOf<Reading>()

    data class Reading(val lat: Double, val lon: Double, val azimuth: Float, val backAzimuth: Float)

    private var currentLat: Double? = null
    private var currentLon: Double? = null
    private var isUserEditing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivArrow = findViewById(R.id.ivArrow)
        etAzimuth = findViewById(R.id.etAzimuth)
        tvBackAzimuth = findViewById(R.id.tvBackAzimuth)
        btnSelect = findViewById(R.id.btnSelect)
        btnReset = findViewById(R.id.btnReset)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        loadState()
        handleIntent(intent)

        etAzimuth.setOnFocusChangeListener { _, hasFocus ->
            isUserEditing = hasFocus
        }

        etAzimuth.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUserEditing) {
                    val azimuthStr = s.toString()
                    if (azimuthStr.isNotEmpty()) {
                        try {
                            val azimuth = azimuthStr.toFloat()
                            if (azimuth in 0f..360f) {
                                currentAzimuth = azimuth
                                updateBackAzimuthDisplay()
                            }
                        } catch (e: NumberFormatException) {
                            // Ignored
                        }
                    }
                }
            }
        })

        btnSelect.setOnClickListener {
            if (currentLat != null && currentLon != null) {
                val backAzimuth = (currentAzimuth + 180) % 360
                selectedLocations.add(Reading(currentLat!!, currentLon!!, currentAzimuth, backAzimuth))
                saveState()
                updateResetButton()
                Toast.makeText(this, "Reading saved. Drawing on Map...", Toast.LENGTH_SHORT).show()
                exportAndShowGpx()
                finish() // Will return to OsmAnd map seamlessly with the GPX intent above it
            } else {
                Toast.makeText(this, "No location selected from OsmAnd", Toast.LENGTH_SHORT).show()
            }
        }

        btnReset.setOnClickListener {
            selectedLocations.clear()
            saveState()
            updateResetButton()
            Toast.makeText(this, "Reset points", Toast.LENGTH_SHORT).show()
        }

        updateResetButton()
    }

    private fun saveState() {
        val sharedPrefs = getSharedPreferences("triangulation_prefs", Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (reading in selectedLocations) {
            val obj = JSONObject()
            obj.put("lat", reading.lat)
            obj.put("lon", reading.lon)
            obj.put("azimuth", reading.azimuth.toDouble())
            obj.put("backAzimuth", reading.backAzimuth.toDouble())
            jsonArray.put(obj)
        }
        sharedPrefs.edit().putString("locations", jsonArray.toString()).apply()
    }

    private fun loadState() {
        val sharedPrefs = getSharedPreferences("triangulation_prefs", Context.MODE_PRIVATE)
        val jsonString = sharedPrefs.getString("locations", null)
        selectedLocations.clear()
        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    selectedLocations.add(
                        Reading(
                            obj.getDouble("lat"),
                            obj.getDouble("lon"),
                            obj.getDouble("azimuth").toFloat(),
                            obj.getDouble("backAzimuth").toFloat()
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateBackAzimuthDisplay() {
        val backAzimuth = (currentAzimuth + 180) % 360
        tvBackAzimuth.text = "Back-Azimuth: ${String.format("%.1f", backAzimuth)}°"
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val latExtra = intent?.getDoubleExtra("lat", Double.NaN) ?: Double.NaN
        val lonExtra = intent?.getDoubleExtra("lon", Double.NaN) ?: Double.NaN

        if (!latExtra.isNaN() && !lonExtra.isNaN()) {
            currentLat = latExtra
            currentLon = lonExtra
            return
        }

        intent?.data?.let { uri ->
            if (uri.scheme == "geo") {
                val ssp = uri.schemeSpecificPart
                val parts = ssp.split(",")
                if (parts.size >= 2) {
                    try {
                        currentLat = parts[0].toDouble()
                        currentLon = parts[1].toDouble()
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun updateResetButton() {
        btnReset.text = "Reset (${selectedLocations.size} locations)"
    }

    private fun exportAndShowGpx() {
        if (selectedLocations.isEmpty()) return

        val gpxStr = StringBuilder()
        gpxStr.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        gpxStr.append("<gpx version=\"1.1\" creator=\"Geolocation Triangulation\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")

        gpxStr.append("  <trk>\n")
        gpxStr.append("    <name>Triangulation Lines</name>\n")
        for (reading in selectedLocations) {
            gpxStr.append("    <trkseg>\n")
            gpxStr.append("      <trkpt lat=\"${reading.lat}\" lon=\"${reading.lon}\"></trkpt>\n")

            val dist = 50.0 // 50 km
            val earthRadius = 6371.0

            val lat1 = Math.toRadians(reading.lat)
            val lon1 = Math.toRadians(reading.lon)
            val brng = Math.toRadians(reading.backAzimuth.toDouble())

            var lat2 = Math.asin(Math.sin(lat1) * Math.cos(dist / earthRadius) +
                    Math.cos(lat1) * Math.sin(dist / earthRadius) * Math.cos(brng))
            var lon2 = lon1 + Math.atan2(Math.sin(brng) * Math.sin(dist / earthRadius) * Math.cos(lat1),
                    Math.cos(dist / earthRadius) - Math.sin(lat1) * Math.sin(lat2))

            lat2 = Math.toDegrees(lat2)
            lon2 = Math.toDegrees(lon2)

            gpxStr.append("      <trkpt lat=\"${lat2}\" lon=\"${lon2}\"></trkpt>\n")
            gpxStr.append("    </trkseg>\n")
        }
        gpxStr.append("  </trk>\n")

        for (reading in selectedLocations) {
            gpxStr.append("  <wpt lat=\"${reading.lat}\" lon=\"${reading.lon}\">\n")
            gpxStr.append("    <name>Back Azimuth ${String.format("%.1f", reading.backAzimuth)}°</name>\n")
            gpxStr.append("  </wpt>\n")
        }

        gpxStr.append("</gpx>\n")

        try {
            val file = File(cacheDir, "triangulation.gpx")
            FileOutputStream(file).use {
                it.write(gpxStr.toString().toByteArray())
            }

            val contentUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(contentUri, "application/gpx+xml")
            intent.setPackage("net.osmand.plus")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to send to OsmAnd", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (isUserEditing) return // Don't update from sensor if user is editing

        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            var azimuthInRadians = orientation[0]
            var azimuthInDegrees = Math.toDegrees(azimuthInRadians.toDouble()).toFloat()
            if (azimuthInDegrees < 0) {
                azimuthInDegrees += 360f
            }

            currentAzimuth = azimuthInDegrees

            etAzimuth.setText(String.format("%.1f", azimuthInDegrees))
            updateBackAzimuthDisplay()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
