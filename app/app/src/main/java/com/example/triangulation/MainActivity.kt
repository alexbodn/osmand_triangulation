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
import android.graphics.Color
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
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
    private lateinit var etDistance: EditText

    private var baseAzimuth = 0f // The raw or user-inputted azimuth BEFORE declination
    private var selectedLocations = mutableListOf<Reading>()

    data class Reading(val lat: Double, val lon: Double, val azimuth: Float, val backAzimuth: Float)

    private var currentLat: Double? = null
    private var currentLon: Double? = null
    private var isUserEditing = false

    private lateinit var osmandHelper: OsmAndAidlHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)

            ivArrow = findViewById(R.id.ivArrow)
            etAzimuth = findViewById(R.id.etAzimuth)
            tvBackAzimuth = findViewById(R.id.tvBackAzimuth)
            tvDeclination = findViewById(R.id.tvDeclination)
            btnSelect = findViewById(R.id.btnSelect)
            btnReset = findViewById(R.id.btnReset)
            btnRegisterOsmAnd = findViewById(R.id.btnRegisterOsmAnd)
            cbMagnetic = findViewById(R.id.cbMagnetic)
            etDistance = findViewById(R.id.etDistance)

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

            osmandHelper = OsmAndAidlHelper(application, this)
            val bound = osmandHelper.bindService()
            if (!bound) {
                Log.w("Triangulation", "Failed to initially bind to OsmAnd service")
            }

            loadState()
            handleIntent(intent)

            etAzimuth.setOnFocusChangeListener { _, hasFocus ->
                isUserEditing = hasFocus
                if (!hasFocus) {
                    updateBackAzimuthDisplay(true)
                }
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
                                    var declination = 0f
                                    if (cbMagnetic.isChecked) {
                                        declination = calculateCurrentDeclination()
                                    }
                                    baseAzimuth = azimuth - declination
                                    if (baseAzimuth < 0) baseAzimuth += 360f
                                    if (baseAzimuth >= 360) baseAzimuth -= 360f

                                    updateBackAzimuthDisplay(false)
                                }
                            } catch (e: NumberFormatException) {
                            }
                        }
                    }
                }
            })

            cbMagnetic.setOnCheckedChangeListener { _, isChecked ->
                val sharedPrefs = getSharedPreferences("triangulation_prefs", Context.MODE_PRIVATE)
                sharedPrefs.edit().putBoolean("isMagneticChecked", isChecked).apply()
                updateBackAzimuthDisplay(true)
            }

            btnSelect.setOnClickListener {
                if (currentLat != null && currentLon != null) {
                    var azimuthToUse = baseAzimuth

                    if (cbMagnetic.isChecked) {
                        azimuthToUse += calculateCurrentDeclination()
                        if (azimuthToUse >= 360f) azimuthToUse -= 360f
                        if (azimuthToUse < 0f) azimuthToUse += 360f
                    }

                    val backAzimuth = (azimuthToUse + 180) % 360
                    selectedLocations.add(Reading(currentLat!!, currentLon!!, azimuthToUse, backAzimuth))
                    saveState()
                    updateResetButton()
                    Toast.makeText(this, "Reading saved. Drawing silently on Map...", Toast.LENGTH_SHORT).show()

                    Handler(Looper.getMainLooper()).postDelayed({
                        drawTriangulationPointsOnMap()

                        var launchIntent = packageManager.getLaunchIntentForPackage("net.osmand.plus")
                            ?: packageManager.getLaunchIntentForPackage("net.osmand")

                        if (selectedLocations.size >= 2) {
                            val cog = calculateCenterOfGravity()
                            if (cog != null) {
                                launchIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:${cog.first},${cog.second}?z=15"))
                                launchIntent.setPackage("net.osmand.plus")
                                if (launchIntent.resolveActivity(packageManager) == null) {
                                    launchIntent.setPackage("net.osmand")
                                }
                            }
                        }

                        if (launchIntent != null) {
                            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(launchIntent)
                        }
                        finish()
                    }, 500)
                } else {
                    Toast.makeText(this, "No location selected from OsmAnd. Launch app from OsmAnd context menu or share.", Toast.LENGTH_SHORT).show()
                }
            }

            btnReset.setOnClickListener {
                selectedLocations.clear()
                saveState()
                updateResetButton()

                // Remove existing line GPX from OsmAnd silently
                osmandHelper.removeGpx("triangulation.gpx")

                Toast.makeText(this, "Reset points and cleared OsmAnd lines", Toast.LENGTH_SHORT).show()

                // Navigate back to OsmAnd immediately
                val launchIntent = packageManager.getLaunchIntentForPackage("net.osmand.plus")
                    ?: packageManager.getLaunchIntentForPackage("net.osmand")
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(launchIntent)
                }
                finish()
            }

            btnRegisterOsmAnd.setOnClickListener {
                val boundService = osmandHelper.bindService()
                if (boundService) {
                    val added = osmandHelper.addContextMenuButton(1001, "Take Back-Azimuth", "")
                    if (added) {
                        Toast.makeText(this, "Successfully registered button to OsmAnd", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to register button to OsmAnd", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Failed to bind to OsmAnd service", Toast.LENGTH_SHORT).show()
                }
            }

            updateResetButton()
        } catch (e: Exception) {
            Log.e("Triangulation", "Error in onCreate", e)
            Toast.makeText(this, "Startup error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun calculateCurrentDeclination(): Float {
        var declinationTargetLat = currentLat ?: 0.0
        var declinationTargetLon = currentLon ?: 0.0

        val cog = calculateCenterOfGravity()
        if (cog != null) {
            declinationTargetLat = cog.first
            declinationTargetLon = cog.second
        } else if (selectedLocations.isNotEmpty()) {
            val fakeCurrentReading = Reading(declinationTargetLat, declinationTargetLon, baseAzimuth, (baseAzimuth + 180f) % 360f)
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
        return geoField.declination
    }

    override fun onOsmAndServiceConnected() {
        Log.d("Triangulation", "OsmAnd Service Connected. Registering Context Menu Button.")
        runOnUiThread {
            Toast.makeText(this, "Connected to OsmAnd API", Toast.LENGTH_SHORT).show()
        }
        osmandHelper.addContextMenuButton(1001, "Take Back-Azimuth", "")
    }

    override fun onOsmAndServiceDisconnected() {
        Log.d("Triangulation", "OsmAnd Service Disconnected.")
        runOnUiThread {
            Toast.makeText(this, "Disconnected from OsmAnd API", Toast.LENGTH_SHORT).show()
        }
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
        val isMagneticChecked = sharedPrefs.getBoolean("isMagneticChecked", false)
        cbMagnetic.isChecked = isMagneticChecked

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

    private fun updateBackAzimuthDisplay(forceUpdateEditText: Boolean = false) {
        var azimuthToDisplay = baseAzimuth

        if (cbMagnetic.isChecked) {
            val declination = calculateCurrentDeclination()
            tvDeclination.visibility = View.VISIBLE
            tvDeclination.text = "Declination applied: ${String.format("%.1f", declination)}°"

            azimuthToDisplay += declination
            if (azimuthToDisplay >= 360f) azimuthToDisplay -= 360f
            if (azimuthToDisplay < 0f) azimuthToDisplay += 360f
        } else {
            tvDeclination.visibility = View.GONE
        }

        if (!isUserEditing || forceUpdateEditText) {
            etAzimuth.setText(String.format("%.1f", azimuthToDisplay))
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

    private fun calculateCenterOfGravity(): Pair<Double, Double>? {
        if (selectedLocations.size < 2) return null
        var totalLat = 0.0
        var totalLon = 0.0
        var count = 0

        for (i in 0 until selectedLocations.size - 1) {
            val r1 = selectedLocations[i]
            val r2 = selectedLocations[i + 1]
            val intersection = calculateIntersection(r1, r2)
            if (intersection != null) {
                totalLat += intersection.first
                totalLon += intersection.second
                count++
            }
        }

        return if (count > 0) Pair(totalLat / count, totalLon / count) else null
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
        if (selectedLocations.isEmpty()) {
            return
        }

        val gpxStr = java.lang.StringBuilder()
        gpxStr.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        gpxStr.append("<gpx version=\"1.1\" creator=\"Geolocation Triangulation\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:osmand=\"https://osmand.net/docs/technical/osmand-file-formats/osmand-gpx\">\n")
        gpxStr.append("  <extensions>\n")
        gpxStr.append("    <osmand:show_start_finish>false</osmand:show_start_finish>\n")
        gpxStr.append("  </extensions>\n")

        var intersection: Pair<Double, Double>? = null
        if (selectedLocations.size >= 2) {
             val r1 = selectedLocations[selectedLocations.size - 2]
             val r2 = selectedLocations[selectedLocations.size - 1]
             intersection = calculateIntersection(r1, r2)
        }

        gpxStr.append("  <trk>\n")
        gpxStr.append("    <name>Triangulation Lines</name>\n")

        // Parse user-provided distance default
        var defaultDist = 3.0
        try {
            val userDist = etDistance.text.toString().toDouble()
            if (userDist > 0) defaultDist = userDist
        } catch (e: Exception) {}

        for (reading in selectedLocations) {
            gpxStr.append("    <trkseg>\n")
            gpxStr.append("      <trkpt lat=\"${reading.lat}\" lon=\"${reading.lon}\"></trkpt>\n")

            val dist = if (selectedLocations.size >= 2) {
                val cog = calculateCenterOfGravity()
                if (cog != null) {
                    calculateDistance(reading.lat, reading.lon, cog.first, cog.second) * 1.5
                } else {
                    defaultDist
                }
            } else {
                defaultDist
            }

            val point2 = calculateDestination(reading.lat, reading.lon, reading.backAzimuth.toDouble(), dist)

            gpxStr.append("      <trkpt lat=\"${point2.first}\" lon=\"${point2.second}\"></trkpt>\n")
            gpxStr.append("    </trkseg>\n")
        }
        gpxStr.append("  </trk>\n")

        for (reading in selectedLocations) {
            // Origin point with binoculars emoji
            val formattedAzimuth = String.format("%.1f", reading.backAzimuth)
            gpxStr.append("  <wpt lat=\"${reading.lat}\" lon=\"${reading.lon}\">\n")
            gpxStr.append("    <sym>none</sym>\n")
            gpxStr.append("    <type>none</type>\n")
            gpxStr.append("    <extensions>\n      <icon>none</icon>\n      <background>null</background>\n    </extensions>\n")
            gpxStr.append("    <name>\uD83D\uDD2D ${formattedAzimuth}°</name>\n")
            gpxStr.append("  </wpt>\n")

            // Destination point (end of line) with cloud emoji
            val dist = if (selectedLocations.size >= 2) {
                val cog = calculateCenterOfGravity()
                if (cog != null) {
                    calculateDistance(reading.lat, reading.lon, cog.first, cog.second) * 1.5
                } else {
                    defaultDist
                }
            } else {
                defaultDist
            }
            val point2 = calculateDestination(reading.lat, reading.lon, reading.backAzimuth.toDouble(), dist)

            gpxStr.append("  <wpt lat=\"${point2.first}\" lon=\"${point2.second}\">\n")
            gpxStr.append("    <sym>none</sym>\n")
            gpxStr.append("    <type>none</type>\n")
            gpxStr.append("    <extensions>\n      <icon>none</icon>\n      <background>null</background>\n    </extensions>\n")
            gpxStr.append("    <name>\u2601\uFE0F</name>\n")
            gpxStr.append("  </wpt>\n")
        }

        val finalCog = calculateCenterOfGravity()
        if (finalCog != null) {
            gpxStr.append("  <wpt lat=\"${finalCog.first}\" lon=\"${finalCog.second}\">\n")
            gpxStr.append("    <sym>none</sym>\n")
            gpxStr.append("    <type>none</type>\n")
            gpxStr.append("    <extensions>\n      <icon>none</icon>\n      <background>null</background>\n    </extensions>\n")
            gpxStr.append("    <name>\uD83E\uDDED Detected Location</name>\n")
            gpxStr.append("  </wpt>\n")
        } else if (intersection != null) {
            gpxStr.append("  <wpt lat=\"${intersection.first}\" lon=\"${intersection.second}\">\n")
            gpxStr.append("    <sym>none</sym>\n")
            gpxStr.append("    <type>none</type>\n")
            gpxStr.append("    <extensions>\n      <icon>none</icon>\n      <background>null</background>\n    </extensions>\n")
            gpxStr.append("    <name>\uD83E\uDDED Detected Location</name>\n")
            gpxStr.append("  </wpt>\n")
        }
        gpxStr.append("</gpx>\n")

        // Pass to AIDL to silently import and display in OsmAnd
        val aidlSuccess = osmandHelper.importGpxFromData(gpxStr.toString(), "triangulation.gpx", "red", true)

        if (!aidlSuccess) {
            Toast.makeText(this, "Failed to send GPX to OsmAnd via AIDL", Toast.LENGTH_SHORT).show()
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
        osmandHelper.unbindService()
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

            baseAzimuth = azimuthInDegrees
            updateBackAzimuthDisplay(false)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
