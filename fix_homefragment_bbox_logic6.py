import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

# Completely rewrite the bbox part to avoid the broken literal newlines embedded in the source file
clean_block = """
        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                if (sharedText.contains("bbox=")) {
                    try {
                        val bboxStrRaw = sharedText.substringAfter("bbox=").substringBefore("&")
                        val regex = Regex("[0-9\\\\.,-]+")
                        val match = regex.find(bboxStrRaw)
                        if (match != null) {
                            val bboxStr = match.value
                            val parts = bboxStr.split(",")
                            if (parts.size >= 4) {
                                val bbox = doubleArrayOf(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
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
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }

                if (!locationParsed && (sharedText.contains("\\"FeatureCollection\\"") || sharedText.contains("\\"Feature\\""))) {
"""

content = re.sub(r'        if \(intent\?\.action == Intent\.ACTION_SEND && intent\?\.type == "text/plain"\) \{\n            val sharedText = intent\?\.getStringExtra\(Intent\.EXTRA_TEXT\)\n            if \(sharedText != null\) \{\n                if \(sharedText\.contains\("bbox="\)\) \{.*?if \(!locationParsed && \(sharedText\.contains\("\\"FeatureCollection\\""\) \|\| sharedText\.contains\("\\"Feature\\""\)\)\) \{', clean_block, content, flags=re.DOTALL)

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
