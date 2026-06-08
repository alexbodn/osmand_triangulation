import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

# Update Reading data class to hold tempDesc
content = content.replace(
    'data class Reading(val lat: Double, val lon: Double, val azimuth: Float, val backAzimuth: Float)',
    'data class Reading(val lat: Double, val lon: Double, val azimuth: Float, val backAzimuth: Float, val tempDesc: String? = null)'
)

# Fix reading instantiations (they don't strictly require fixing since we added a default parameter, but we need to pass tempDesc when saving)
content = content.replace(
    'selectedLocations.add(Reading(currentLat!!, currentLon!!, azimuthToUse, backAzimuth))',
    'selectedLocations.add(Reading(currentLat!!, currentLon!!, azimuthToUse, backAzimuth, rawReceivedParameter?.let { extractDescription(it) }))'
)

# Extract Description function
extract_desc_func = """
    private fun extractDescription(rawText: String): String? {
        // e.g. "האחים אוסטשינסקי (ראשונים) 19א, ראשון לציון\\nLocation: geo:..."
        if (rawText.contains("Location:")) {
            val lines = rawText.split("\\n")
            if (lines.isNotEmpty() && lines[0].isNotBlank() && !lines[0].startsWith("Location:")) {
                return lines[0].trim()
            }
        }
        return null
    }

    private fun handleGeoJson
"""
content = content.replace('    private fun handleGeoJson', extract_desc_func)

# Pre-fill description in showSaveDialog
content = content.replace(
    'private fun showSaveDialog(lat: Double, lon: Double) {',
    'private fun showSaveDialog(lat: Double, lon: Double, initialDesc: String?) {'
)
content = content.replace(
    'val etDesc = view.findViewById<EditText>(R.id.etDialogDesc)',
    'val etDesc = view.findViewById<EditText>(R.id.etDialogDesc)\n        if (initialDesc != null) etDesc.setText(initialDesc)'
)
content = content.replace(
    'showSaveDialog(reading.lat, reading.lon)',
    'showSaveDialog(reading.lat, reading.lon, reading.tempDesc)'
)

# Force switch to Home Tab in handleIntent
force_switch = """
        if (locationParsed) {
            // Switch to Home tab
            (activity as? com.example.triangulation.MainActivity)?.let { mainActivity ->
                val viewPager = mainActivity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
                viewPager?.currentItem = 0
            }

            intent?.removeExtra("lat")
"""
content = content.replace("""        if (locationParsed) {
            intent?.removeExtra("lat")""", force_switch)

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
