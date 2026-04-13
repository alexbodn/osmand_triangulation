package com.example.triangulation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.GeomagneticField
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log
import android.os.Handler
import android.os.Looper
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity(), SensorEventListener, OsmAndAidlHelper.OsmAndAidlListener {

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null

    private lateinit var ivArrow: ImageView
    private lateinit var etAzimuth: EditText
    private lateinit var tvBackAzimuth: TextView
    private lateinit var tvDeclination: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnReset: Button
    private lateinit var btnRegisterOsmAnd: Button
    private lateinit var cbMagnetic: CheckBox

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
        tvDeclination = findViewById(R.id.tvDeclination)
        btnSelect = findViewById(R.id.btnSelect)
        btnReset = findViewById(R.id.btnReset)
        btnRegisterOsmAnd = findViewById(R.id.btnRegisterOsmAnd)
        cbMagnetic = findViewById(R.id.cbMagnetic)

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

        cbMagnetic.setOnCheckedChangeListener { _, _ ->
            updateBackAzimuthDisplay()
        }

        btnSelect.setOnClickListener {
            if (currentLat != null && currentLon != null) {
                var azimuthToUse = currentAzimuth

                if (cbMagnetic.isChecked) {
                    var declinationTargetLat = currentLat!!
                    var declinationTargetLon = currentLon!!

                    if (selectedLocations.isNotEmpty()) {
                        val fakeCurrentReading = Reading(currentLat!!, currentLon!!, currentAzimuth, (currentAzimuth + 180f) % 360f)
                        val r1 = selectedLocations.last()
                        val intersection = calculateIntersection(r1, fakeCurrentReading)
                        if (intersection != null) {
                            declinationTargetLat = intersection.first
                            declinationTargetLon = intersection.second
                        }
                    }

                    val geoField = GeomagneticField(
                        declinationTargetLat.toFloat(),
                        declinationTargetLon.toFloat(),
                        0f,
                        System.currentTimeMillis()
                    )
                    azimuthToUse += geoField.declination
                    if (azimuthToUse >= 360f) azimuthToUse -= 360f
                    if (azimuthToUse < 0f) azimuthToUse += 360f
                }

                val backAzimuth = (azimuthToUse + 180) % 360
                selectedLocations.add(Reading(currentLat!!, currentLon!!, azimuthToUse, backAzimuth))
                saveState()
                updateResetButton()
                Toast.makeText(this, "Reading saved. Silently importing to OsmAnd...", Toast.LENGTH_SHORT).show()
                drawTriangulationPointsOnMap()
                finish()
            } else {
                Toast.makeText(this, "No location selected from OsmAnd. Launch app from OsmAnd context menu or share.", Toast.LENGTH_SHORT).show()
            }
        }

        btnReset.setOnClickListener {
            selectedLocations.clear()
            saveState()
            updateResetButton()
            Toast.makeText(this, "Reset points", Toast.LENGTH_SHORT).show()
            drawTriangulationPointsOnMap() // Redraws empty layer
        }

        btnRegisterOsmAnd.setOnClickListener {
            osmandHelper.addContextMenuButton(1001, "Take Back-Azimuth", "")
            Toast.makeText(this, "Attempted to register button to OsmAnd", Toast.LENGTH_SHORT).show()
        }

        updateResetButton()
    }

    override fun onOsmAndServiceConnected() {
        Log.d("Triangulation", "OsmAnd Service Connected. Registering Context Menu Button.")
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
        var azimuthToDisplay = currentAzimuth
        if (cbMagnetic.isChecked && currentLat != null && currentLon != null) {
            // Use intersection target if possible, otherwise use current
            var declinationTargetLat = currentLat!!
            var declinationTargetLon = currentLon!!

            if (selectedLocations.isNotEmpty()) {
                val fakeCurrentReading = Reading(currentLat!!, currentLon!!, currentAzimuth, (currentAzimuth + 180f) % 360f)
                val r1 = selectedLocations.last()
                val intersection = calculateIntersection(r1, fakeCurrentReading)
                if (intersection != null) {
                    declinationTargetLat = intersection.first
                    declinationTargetLon = intersection.second
                }
            }

            val geoField = GeomagneticField(
                declinationTargetLat.toFloat(),
                declinationTargetLon.toFloat(),
                0f,
                System.currentTimeMillis()
            )

            tvDeclination.visibility = View.VISIBLE
            tvDeclination.text = "Declination applied: ${String.format("%.1f", geoField.declination)}°"

            azimuthToDisplay += geoField.declination
            if (azimuthToDisplay >= 360f) azimuthToDisplay -= 360f
            if (azimuthToDisplay < 0f) azimuthToDisplay += 360f
        } else {
            tvDeclination.visibility = View.GONE
        }

        val backAzimuth = (azimuthToDisplay + 180) % 360
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
                var parsedLat: Double? = null
                var parsedLon: Double? = null

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

                if (parsedLat == null || parsedLon == null) {
                    val fallbackRegex = Regex("([0-9]{1,2}\\.[0-9]+)[^0-9.-]+([0-9]{1,3}\\.[0-9]+)")
                    val fallbackMatch = fallbackRegex.find(sharedText)
                    if (fallbackMatch != null) {
                        try {
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
                    Toast.makeText(this, "Could not parse coordinates from: $sharedText", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateResetButton() {
        btnReset.text = "Reset (${selectedLocations.size} locations)"
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat/2) * sin(dLat/2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon/2) * sin(dLon/2)
        val c = 2 * atan2(Math.sqrt(a), Math.sqrt(1-a))
        return R * c
    }

    private fun calculateDestination(lat: Double, lon: Double, bearing: Double, distanceKm: Double): Pair<Double, Double> {
        val R = 6371.0
        val lat1 = Math.toRadians(lat)
        val lon1 = Math.toRadians(lon)
        val brng = Math.toRadians(bearing)

        var lat2 = asin(sin(lat1) * cos(distanceKm / R) +
                cos(lat1) * sin(distanceKm / R) * cos(brng))
        var lon2 = lon1 + atan2(sin(brng) * sin(distanceKm / R) * cos(lat1),
                cos(distanceKm / R) - sin(lat1) * sin(lat2))

        return Pair(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }

    private fun calculateIntersection(r1: Reading, r2: Reading): Pair<Double, Double>? {
        val lat1 = Math.toRadians(r1.lat)
        val lon1 = Math.toRadians(r1.lon)
        val brng1 = Math.toRadians(r1.backAzimuth.toDouble())

        val lat2 = Math.toRadians(r2.lat)
        val lon2 = Math.toRadians(r2.lon)
        val brng2 = Math.toRadians(r2.backAzimuth.toDouble())

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val dist12 = 2 * asin(Math.sqrt(sin(dLat/2)*sin(dLat/2) + cos(lat1)*cos(lat2)*sin(dLon/2)*sin(dLon/2)))
        if (dist12 == 0.0) return null

        val brngA = Math.acos((sin(lat2) - sin(lat1)*cos(dist12)) / (sin(dist12)*cos(lat1)))
        val brngB = Math.acos((sin(lat1) - sin(lat2)*cos(dist12)) / (sin(dist12)*cos(lat2)))

        val brng12 = if (sin(lon2-lon1) > 0) brngA else 2*Math.PI - brngA
        val brng21 = if (sin(lon2-lon1) > 0) 2*Math.PI - brngB else brngB

        val alpha1 = (brng1 - brng12 + Math.PI) % (2*Math.PI) - Math.PI
        val alpha2 = (brng21 - brng2 + Math.PI) % (2*Math.PI) - Math.PI

        if (sin(alpha1) == 0.0 && sin(alpha2) == 0.0) return null
        if (sin(alpha1)*sin(alpha2) < 0) return null

        val alpha3 = Math.acos(-cos(alpha1)*cos(alpha2) + sin(alpha1)*sin(alpha2)*cos(dist12))
        val dist13 = atan2(sin(dist12)*sin(alpha1)*sin(alpha2), cos(alpha2) + cos(alpha1)*cos(alpha3))

        val lat3 = asin(sin(lat1)*cos(dist13) + cos(lat1)*sin(dist13)*cos(brng1))
        val dLon13 = atan2(sin(brng1)*sin(dist13)*cos(lat1), cos(dist13) - sin(lat1)*sin(lat3))
        val lon3 = lon1 + dLon13

        return Pair(Math.toDegrees(lat3), Math.toDegrees(lon3))
    }

    private fun drawTriangulationPointsOnMap() {
        val gpxStr = java.lang.StringBuilder()
        gpxStr.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        gpxStr.append("<gpx version=\"1.1\" creator=\"Geolocation Triangulation\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")

        var intersection: Pair<Double, Double>? = null
        if (selectedLocations.size >= 2) {
             val r1 = selectedLocations[selectedLocations.size - 2]
             val r2 = selectedLocations[selectedLocations.size - 1]
             intersection = calculateIntersection(r1, r2)
        }

        gpxStr.append("  <trk>\n")
        gpxStr.append("    <name>Triangulation Lines</name>\n")
        for (reading in selectedLocations) {
            gpxStr.append("    <trkseg>\n")
            gpxStr.append("      <trkpt lat=\"${reading.lat}\" lon=\"${reading.lon}\"></trkpt>\n")

            val dist = if (intersection != null) {
                calculateDistance(reading.lat, reading.lon, intersection.first, intersection.second) * 1.5
            } else {
                50.0 // Default 50km
            }

            val point2 = calculateDestination(reading.lat, reading.lon, reading.backAzimuth.toDouble(), dist)

            gpxStr.append("      <trkpt lat=\"${point2.first}\" lon=\"${point2.second}\"></trkpt>\n")
            gpxStr.append("    </trkseg>\n")
        }
        gpxStr.append("  </trk>\n")

        for (reading in selectedLocations) {
            gpxStr.append("  <wpt lat=\"${reading.lat}\" lon=\"${reading.lon}\">\n")
            gpxStr.append("    <name>Back Azimuth ${String.format("%.1f", reading.backAzimuth)}°</name>\n")
            gpxStr.append("  </wpt>\n")
        }

        if (intersection != null) {
            gpxStr.append("  <wpt lat=\"${intersection.first}\" lon=\"${intersection.second}\">\n")
            gpxStr.append("    <name>Intersection</name>\n")
            gpxStr.append("  </wpt>\n")
        }

        gpxStr.append("</gpx>\n")

        // Pass to AIDL to silently import and display
        osmandHelper.importGpxFromData(gpxStr.toString(), "triangulation.gpx", "red", true)
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
        osmandHelper.unbindService() // Prevent memory leak by correctly unbinding the AIDL service
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
