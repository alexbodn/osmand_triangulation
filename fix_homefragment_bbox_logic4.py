import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

replacement_block = """
        if (intent?.action == Intent.ACTION_SEND && intent?.type == "text/plain") {
            val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                if (sharedText.contains("bbox=")) {
                    try {
                        var bboxStr = sharedText.substringAfter("bbox=").substringBefore("&")
                        // Clean up newlines, returns, and take the first token
                        bboxStr = bboxStr.replace("\\n", " ").replace("\\r", " ").trim().split(" ")[0]
                        val parts = bboxStr.split(",")
                        if (parts.size == 4) {
                            val bbox = doubleArrayOf(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())
                            // Switch to locations tab
"""

content = re.sub(r'        if \(intent\?\.action == Intent\.ACTION_SEND && intent\?\.type == "text/plain"\) \{\n            val sharedText = intent\?\.getStringExtra\(Intent\.EXTRA_TEXT\)\n            if \(sharedText != null\) \{\n                if \(sharedText\.contains\("bbox="\)\) \{\n                    try \{\n                        val bboxStr = sharedText\.substringAfter\("bbox="\)\.substringBefore\("&"\)\.replace\("\\\\n", " "\)\.replace\("\\\\r", " "\)\.split\(" "\)\[0\]\n                        val parts = bboxStr\.split\(","\)\n                        if \(parts\.size == 4\) \{\n                            val bbox = doubleArrayOf\(parts\[0\]\.toDouble\(\), parts\[1\]\.toDouble\(\), parts\[2\]\.toDouble\(\), parts\[3\]\.toDouble\(\)\)\n                            // Switch to locations tab', replacement_block, content, flags=re.DOTALL)

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
