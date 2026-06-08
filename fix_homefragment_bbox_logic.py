import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

new_logic = """
        var locationParsed = false
        var bboxParsed = false

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
                            // Switch to locations tab
                            (activity as? com.example.triangulation.MainActivity)?.let { mainActivity ->
                                val viewPager = mainActivity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
                                viewPager?.currentItem = 1
                            }

                            val sharedPrefs = requireActivity().getSharedPreferences("triangulation_prefs", android.content.Context.MODE_PRIVATE)
                            sharedPrefs.edit().putString("bbox_filter", bbox.joinToString(",")).apply()

                            // Tell LocationsFragment to check if it's already alive
                            val bboxIntent = Intent("com.example.triangulation.BBOX_FILTER")
                            requireContext().sendBroadcast(bboxIntent)
                            bboxParsed = true
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
        }


        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                if (sharedText.contains("bbox=")) {
                    try {
                        // Support URLs containing bbox=... within shared text (sometimes with \r\n or spaces following)
                        val bboxStr = sharedText.substringAfter("bbox=").substringBefore("&").substringBefore("\\n").substringBefore("\\r").substringBefore(" ")
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
                            bboxParsed = true
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                if (!locationParsed && (sharedText.contains("\\"FeatureCollection\\"") || sharedText.contains("\\"Feature\\""))) {
"""

content = re.sub(r'        var locationParsed = false\n\n        if \(!latExtra\.isNaN\(\) && !lonExtra\.isNaN\(\)\) \{.*?if \(!locationParsed && \(sharedText\.contains\("\\"FeatureCollection\\""\) \|\| sharedText\.contains\("\\"Feature\\""\)\)\) \{', new_logic, content, flags=re.DOTALL)

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
