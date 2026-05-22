import re

with open('/tmp/HomeFragment.kt', 'r') as f:
    content = f.read()

new_handle_intent = """
    private fun handleIntent(intent: Intent?) {
        val latExtra = intent?.getDoubleExtra("lat", Double.NaN) ?: Double.NaN
        val lonExtra = intent?.getDoubleExtra("lon", Double.NaN) ?: Double.NaN

        var locationParsed = false

        if (!latExtra.isNaN() && !lonExtra.isNaN()) {
            currentLat = latExtra
            currentLon = lonExtra
            rawReceivedParameter = "lat=$latExtra, lon=$lonExtra"
            locationParsed = true
        }

        if (!locationParsed) {
            intent?.data?.let { uri ->
                if (uri.scheme == "geo") {
                    rawReceivedParameter = uri.toString()
                    val ssp = uri.schemeSpecificPart
                    val mainPart = ssp.substringBefore('?')
                    val parts = mainPart.split(",")
                    if (parts.size >= 2) {
                        try {
                            currentLat = parts[0].toDouble()
                            currentLon = parts[1].toDouble()
                            locationParsed = true
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    }
                } else if (uri.toString().contains("bbox=")) {
                    // Example: ...#map=15/lat/lon&bbox=minLon,minLat,maxLon,maxLat
                    try {
                        val bboxStr = uri.toString().substringAfter("bbox=").substringBefore("&")
                        val parts = bboxStr.split(",")
                        if (parts.size == 4) {
                            val bbox = doubleArrayOf(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
                            // Switch to locations tab and pass bbox
                            (activity as? com.example.triangulation.MainActivity)?.let { mainActivity ->
                                val viewPager = mainActivity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
                                viewPager?.currentItem = 1

                                val fragment = (mainActivity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager).adapter as? com.example.triangulation.ui.ViewPagerAdapter)?.createFragment(1)
                                if (fragment is LocationsFragment) {
                                     // This is a hacky way since createFragment makes a new one, better to broadcast or use ViewModel
                                     // Alternatively, we can save bbox in activity/sharedprefs. Let's do sharedViewModel or static/broadcast.
                                }
                            }
                            // Using a simple broadcast for now
                            val bboxIntent = Intent("com.example.triangulation.BBOX_FILTER")
                            bboxIntent.putExtra("bbox", bbox)
                            requireContext().sendBroadcast(bboxIntent)
                            locationParsed = true
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }

        if (!locationParsed && intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                if (sharedText.contains("\"type\": \"FeatureCollection\"") || sharedText.contains("\"type\": \"Feature\"")) {
                    handleGeoJson(sharedText)
                    locationParsed = true
                } else {
                    rawReceivedParameter = sharedText
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
                        locationParsed = true
                    }
                }
            }
        }

        if (locationParsed) {
            intent?.removeExtra("lat")
            intent?.removeExtra("lon")
            intent?.removeExtra(Intent.EXTRA_TEXT)
            intent?.data = null
            intent?.action = null
            requireActivity().intent.replaceExtras(Bundle())
        }
    }

    private fun handleGeoJson(jsonStr: String) {
        try {
            val root = JSONObject(jsonStr)
            val features = if (root.has("features")) root.getJSONArray("features") else JSONArray().apply { put(root) }
            val parsedLocs = mutableListOf<com.example.triangulation.data.LibraryLocation>()

            for (i in 0 until features.length()) {
                val feature = features.getJSONObject(i)
                if (feature.has("geometry")) {
                    val geom = feature.getJSONObject("geometry")
                    if (geom.optString("type") == "Point") {
                        val coords = geom.getJSONArray("coordinates")
                        val lon = coords.getDouble(0)
                        val lat = coords.getDouble(1)
                        var desc: String? = null
                        var img: String? = null

                        if (feature.has("properties")) {
                            val props = feature.getJSONObject("properties")
                            desc = props.optString("name", props.optString("desc", null))
                            img = props.optString("image", props.optString("img", null))
                        }
                        parsedLocs.add(com.example.triangulation.data.LibraryLocation(lat, lon, desc, img))
                    }
                }
            }

            if (parsedLocs.isNotEmpty()) {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Import GeoJSON")
                    .setMessage("Found ${parsedLocs.size} locations. Do you want to import them into the library?")
                    .setPositiveButton("Import") { _, _ ->
                        var importedCount = 0
                        for (loc in parsedLocs) {
                            if (libraryManager.isLocationInLibrary(loc.lat, loc.lon) == null) {
                                libraryManager.addLocation(loc)
                                importedCount++
                            }
                        }
                        Toast.makeText(requireContext(), "Imported $importedCount new locations.", Toast.LENGTH_SHORT).show()

                        // Switch to locations tab
                        (activity as? com.example.triangulation.MainActivity)?.let { mainActivity ->
                            val viewPager = mainActivity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
                            viewPager?.currentItem = 1
                        }
                        val refreshIntent = Intent("com.example.triangulation.REFRESH_LIBRARY")
                        requireContext().sendBroadcast(refreshIntent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to parse GeoJSON", Toast.LENGTH_SHORT).show()
        }
    }
"""

content = re.sub(r'    private fun handleIntent\(intent: Intent\?\) \{.*?(?=    private fun updatePointsList)', new_handle_intent, content, flags=re.DOTALL)

with open('/tmp/HomeFragment.kt', 'w') as f:
    f.write(content)
