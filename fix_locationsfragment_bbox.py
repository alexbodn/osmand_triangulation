import re

with open('app/app/src/main/java/com/example/triangulation/ui/LocationsFragment.kt', 'r') as f:
    content = f.read()

new_loaddata = """
    fun loadData() {
        var locations = libraryManager.getLocations()

        // Check shared prefs first for cold start bbox filtering
        val sharedPrefs = requireActivity().getSharedPreferences("triangulation_prefs", android.content.Context.MODE_PRIVATE)
        val savedBboxStr = sharedPrefs.getString("bbox_filter", null)
        if (savedBboxStr != null) {
            val parts = savedBboxStr.split(",")
            if (parts.size == 4) {
                bboxFilter = doubleArrayOf(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
                // clear it so it only applies once
                sharedPrefs.edit().remove("bbox_filter").apply()
            }
        }

        bboxFilter?.let { bbox ->
            val minLon = bbox[0]
            val minLat = bbox[1]
            val maxLon = bbox[2]
            val maxLat = bbox[3]

            locations = locations.filter {
                it.lon in minLon..maxLon && it.lat in minLat..maxLat
            }
        }

        adapter.updateData(locations)
    }
"""

content = re.sub(r'    fun loadData\(\) \{.*?(?=    override fun onOsmAndServiceConnected)', new_loaddata, content, flags=re.DOTALL)

# Fix RECEIVER_NOT_EXPORTED compatibility
content = content.replace('requireContext().registerReceiver(updateReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)', 'androidx.core.content.ContextCompat.registerReceiver(requireContext(), updateReceiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)')

with open('app/app/src/main/java/com/example/triangulation/ui/LocationsFragment.kt', 'w') as f:
    f.write(content)
