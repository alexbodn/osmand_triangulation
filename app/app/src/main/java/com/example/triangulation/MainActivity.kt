package com.example.triangulation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import android.util.Log
import android.os.Handler
import android.os.Looper

class MainActivity : AppCompatActivity(), SensorEventListener, OsmAndAidlHelper.OsmAndAidlListener {

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null

    private lateinit var ivArrow: ImageView
    private lateinit var etAzimuth: EditText
    private lateinit var tvBackAzimuth: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnReset: Button
    private lateinit var btnRegisterOsmAnd: Button

    private var currentAzimuth = 0f
    private var selectedLocations = mutableListOf<Reading>()

    data class Reading(val lat: Double, val lon: Double, val azimuth: Float, val backAzimuth: Float)

    private var currentLat: Double? = null
    private var currentLon: Double? = null
    private var isUserEditing = false

    private lateinit var osmandHelper: OsmAndAidlHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivArrow = findViewById(R.id.ivArrow)
        etAzimuth = findViewById(R.id.etAzimuth)
        tvBackAzimuth = findViewById(R.id.tvBackAzimuth)
        btnSelect = findViewById(R.id.btnSelect)
        btnReset = findViewById(R.id.btnReset)
        btnRegisterOsmAnd = findViewById(R.id.btnRegisterOsmAnd)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        osmandHelper = OsmAndAidlHelper(application, this)
        osmandHelper.bindService()

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
                Toast.makeText(this, "No location selected from OsmAnd. Launch app from OsmAnd context menu or share.", Toast.LENGTH_SHORT).show()
            }
        }

        btnReset.setOnClickListener {
            selectedLocations.clear()
            saveState()
            updateResetButton()
            Toast.makeText(this, "Reset points", Toast.LENGTH_SHORT).show()
            exportAndShowGpx()
        }

        btnRegisterOsmAnd.setOnClickListener {
            // Registering with an empty layer ID usually acts as a global context menu
            osmandHelper.addContextMenuButton(1001, "Take Back-Azimuth", "")
            Toast.makeText(this, "Attempted to register button to OsmAnd", Toast.LENGTH_SHORT).show()
        }

        updateResetButton()
    }

    override fun onOsmAndServiceConnected() {
        Log.d("Triangulation", "OsmAnd Service Connected. Registering Context Menu Button.")
        // Empty string for layer ID acts as a global registration
        osmandHelper.addContextMenuButton(1001, "Take Back-Azimuth", "")
    }

    override fun onOsmAndServiceDisconnected() {
        Log.d("Triangulation", "OsmAnd Service Disconnected.")
    }

    override fun onContextMenuButtonClicked(buttonId: Int, pointId: String?, layerId: String?) {
        Log.d("Triangulation", "Context Menu Button Clicked: $buttonId, $pointId, $layerId")

        Handler(Looper.getMainLooper()).post {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

            try {
                if (pointId != null && pointId.contains(",")) {
                    val parts = pointId.split(",")
                    val lat = parts[0].toDouble()
                    val lon = parts[1].toDouble()
                    intent.putExtra("lat", lat)
                    intent.putExtra("lon", lon)
                } else {
                    intent.putExtra("pointId", pointId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            startActivity(intent)
        }
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
                // Geo URI looks like geo:lat,lon?z=15
                val mainPart = ssp.substringBefore('?')
                val parts = mainPart.split(",")
                if (parts.size >= 2) {
                    try {
                        currentLat = parts[0].toDouble()
                        currentLon = parts[1].toDouble()
                        return
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Handle SEND intents (Share from OsmAnd)
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                Log.d("Triangulation", "Received shared text: $sharedText")
                var parsedLat: Double? = null
                var parsedLon: Double? = null

                // 1. Try to find http://osmand.net/go?lat=X&lon=Y
                val latRegex = Regex("lat=([0-9.-]+)")
                val lonRegex = Regex("lon=([0-9.-]+)")
                val latMatch = latRegex.find(sharedText)
                val lonMatch = lonRegex.find(sharedText)

                if (latMatch != null && lonMatch != null) {
                    try {
                        parsedLat = latMatch.groupValues[1].toDouble()
                        parsedLon = lonMatch.groupValues[1].toDouble()
                    } catch (e: Exception) { e.printStackTrace() }
                }

                // 2. Try to find a geo: URI in the shared text
                if (parsedLat == null || parsedLon == null) {
                    val geoRegex = Regex("geo:([0-9.-]+),([0-9.-]+)")
                    val geoMatch = geoRegex.find(sharedText)
                    if (geoMatch != null) {
                        try {
                            parsedLat = geoMatch.groupValues[1].toDouble()
                            parsedLon = geoMatch.groupValues[2].toDouble()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }

                // 3. Try to just find any two decimals that look like coordinates
                // e.g. "My Location is 37.123, -122.456"
                if (parsedLat == null || parsedLon == null) {
                    val fallbackRegex = Regex("([0-9]{1,2}\\.[0-9]+)[^0-9.-]+([0-9]{1,3}\\.[0-9]+)")
                    val fallbackMatch = fallbackRegex.find(sharedText)
                    if (fallbackMatch != null) {
                        try {
                            // Quick sanity check - check for negative signs before the matched numbers just in case
                            val latStr = sharedText.substring(Math.max(0, fallbackMatch.groups[1]!!.range.first - 1), fallbackMatch.groups[1]!!.range.last + 1)
                            val lonStr = sharedText.substring(Math.max(0, fallbackMatch.groups[2]!!.range.first - 1), fallbackMatch.groups[2]!!.range.last + 1)

                            parsedLat = if(latStr.startsWith("-")) -1 * fallbackMatch.groupValues[1].toDouble() else fallbackMatch.groupValues[1].toDouble()
                            parsedLon = if(lonStr.startsWith("-")) -1 * fallbackMatch.groupValues[2].toDouble() else fallbackMatch.groupValues[2].toDouble()

                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }

                if (parsedLat != null && parsedLon != null) {
                    currentLat = parsedLat
                    currentLon = parsedLon
                    Toast.makeText(this, "Received coordinates: $currentLat, $currentLon", Toast.LENGTH_SHORT).show()
                } else {
                    // Fallback to show user the failure
                    Toast.makeText(this, "Could not parse coordinates from: $sharedText", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateResetButton() {
        btnReset.text = "Reset (${selectedLocations.size} locations)"
    }

    private fun getOsmandPackage(): String {
        val pm = packageManager
        return try {
            pm.getPackageInfo("net.osmand.plus", PackageManager.GET_META_DATA)
            "net.osmand.plus"
        } catch (e: PackageManager.NameNotFoundException) {
            "net.osmand"
        }
    }

    private fun exportAndShowGpx() {
        val gpxStr = java.lang.StringBuilder()
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
            intent.setPackage(getOsmandPackage())
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

    override fun onDestroy() {
        super.onDestroy()
        // Removed unbindService() to allow the AIDL listener to stay alive
        // for background context menu button clicks.
        // It's a hack, but without a dedicated foreground service, this ensures the callback works
        // while the app process is alive.
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
