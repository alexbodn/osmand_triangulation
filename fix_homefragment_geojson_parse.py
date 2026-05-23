import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

new_parse = """    private fun handleGeoJson(jsonStr: String) {
        try {
            // Extract the actual JSON from the string, in case it contains mixed text (like URLs or labels)
            val startIndex = jsonStr.indexOf("{")
            val endIndex = jsonStr.lastIndexOf("}")

            if (startIndex == -1 || endIndex == -1 || startIndex > endIndex) {
                throw Exception("No JSON object found in text")
            }

            val pureJsonStr = jsonStr.substring(startIndex, endIndex + 1)
            val root = JSONObject(pureJsonStr)"""

content = re.sub(r'    private fun handleGeoJson\s*\(\w+:\s*String\)\s*\{\s*try\s*\{\s*val root = JSONObject\(jsonStr\)', new_parse, content, flags=re.DOTALL)

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
