import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

# Replace broadcast with ViewModel logic for bbox. But wait, I'll just use SharedPreferences to pass the bbox so the LocationsFragment can pick it up when it starts.
# That avoids race conditions and viewmodel dependencies completely.

bbox_logic = """
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
                            locationParsed = true
"""

content = re.sub(r'                            val bbox = doubleArrayOf\(parts\[0\].toDouble\(\), parts\[1\].toDouble\(\), parts\[2\].toDouble\(\), parts\[3\].toDouble\(\)\).*?locationParsed = true', bbox_logic, content, flags=re.DOTALL)

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
