import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

new_handle_intent = """
        if (!locationParsed && intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                if (sharedText.contains("bbox=")) {
                    try {
                        val bboxStr = sharedText.substringAfter("bbox=").substringBefore("&").substringBefore("\\n").substringBefore(" ")
                        val parts = bboxStr.split(",")
                        if (parts.size == 4) {
                            val bbox = doubleArrayOf(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
                            // Switch to locations tab
                            (activity as? com.example.triangulation.MainActivity)?.let { mainActivity ->
                                val viewPager = mainActivity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
                                viewPager?.currentItem = 1
                            }

                            val sharedPrefs = requireActivity().getSharedPreferences("triangulation_prefs", android.content.Context.MODE_PRIVATE)
                            sharedPrefs.edit().putString("bbox_filter", bbox.joinToString(",")).apply()

                            val bboxIntent = Intent("com.example.triangulation.BBOX_FILTER")
                            requireContext().sendBroadcast(bboxIntent)
                            locationParsed = true
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                if (!locationParsed && (sharedText.contains("\\"FeatureCollection\\"") || sharedText.contains("\\"Feature\\""))) {
                    handleGeoJson(sharedText)
                    locationParsed = true
                } else if (!locationParsed) {
"""

content = re.sub(r'        if \(!locationParsed && intent\?\.action == Intent\.ACTION_SEND && intent\?\.type == "text/plain"\) \{\n            val sharedText = intent\?\.getStringExtra\(Intent\.EXTRA_TEXT\)\n            if \(sharedText != null\) \{\n\n                if \(sharedText\.contains\("\\"FeatureCollection\\""\) \|\| sharedText\.contains\("\\"Feature\\""\)\) \{\n\n                    handleGeoJson\(sharedText\)\n                    locationParsed = true\n                \} else \{', new_handle_intent, content, flags=re.DOTALL)

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
