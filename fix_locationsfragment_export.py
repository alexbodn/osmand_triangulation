import re

with open('app/app/src/main/java/com/example/triangulation/ui/LocationsFragment.kt', 'r') as f:
    content = f.read()

# Add export buttons wiring in onViewCreated
export_logic = """
        val btnExportAll = view.findViewById<android.widget.Button>(R.id.btnExportAll)
        val btnExportBounded = view.findViewById<android.widget.Button>(R.id.btnExportBounded)

        btnExportAll.setOnClickListener {
            exportGeoJson(libraryManager.getLocations())
        }

        btnExportBounded.setOnClickListener {
            val allLocs = libraryManager.getLocations()
            val filtered = if (bboxFilter != null) {
                val bbox = bboxFilter!!
                allLocs.filter { it.lon in bbox[0]..bbox[2] && it.lat in bbox[1]..bbox[3] }
            } else {
                allLocs
            }
            exportGeoJson(filtered)
        }
"""

content = re.sub(r'        rvLocations\.adapter = adapter\n', '        rvLocations.adapter = adapter\n' + export_logic, content)

# Add exportGeoJson function
export_func = """
    private fun exportGeoJson(locs: List<LibraryLocation>) {
        val features = org.json.JSONArray()
        for (loc in locs) {
            val feature = org.json.JSONObject()
            feature.put("type", "Feature")

            val geometry = org.json.JSONObject()
            geometry.put("type", "Point")
            val coords = org.json.JSONArray()
            coords.put(loc.lon)
            coords.put(loc.lat)
            geometry.put("coordinates", coords)
            feature.put("geometry", geometry)

            val properties = org.json.JSONObject()
            if (loc.desc != null) properties.put("desc", loc.desc)
            if (loc.img != null) properties.put("img", loc.img)
            feature.put("properties", properties)

            features.put(feature)
        }

        val root = org.json.JSONObject()
        root.put("type", "FeatureCollection")
        root.put("features", features)

        val jsonStr = root.toString()

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, jsonStr)
        startActivity(Intent.createChooser(intent, "Export Locations"))
    }
"""

content = content.replace('    override fun onOsmAndServiceConnected() {}', export_func + '\n    override fun onOsmAndServiceConnected() {}')

with open('app/app/src/main/java/com/example/triangulation/ui/LocationsFragment.kt', 'w') as f:
    f.write(content)
