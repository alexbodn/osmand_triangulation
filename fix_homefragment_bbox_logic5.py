import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

# Replace the broken string entirely
good_line = '                        var bboxStr = sharedText.substringAfter("bbox=").substringBefore("&").replace("\\n", " ").replace("\\r", " ").trim().split(" ")[0]'
content = re.sub(r'                        val bboxStr = sharedText\.substringAfter\("bbox="\)\.substringBefore\("&"\)\.replace\("\n", " "\)\.replace\("", " "\)\.split\(" "\)\[0\]', good_line, content)

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
