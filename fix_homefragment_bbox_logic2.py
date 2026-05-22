import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

# Fix the broken lines caused by `\r\n` newlines within the kotlin code string
content = content.replace('substringBefore("\\\\r").substringBefore(" ")', 'substringBefore("\\\\r").substringBefore(" ")')
# The python replacement earlier had literal newlines from the script `substringBefore("\n")` instead of `\\n`.

# Actually I'll just write it correctly.
target = """
                        // Support URLs containing bbox=... within shared text (sometimes with \\r\\n or spaces following)
                        val bboxStr = sharedText.substringAfter("bbox=").substringBefore("&").substringBefore("\\n").substringBefore("\\r").substringBefore(" ")
                        val parts = bboxStr.split(",")
"""

# Find what's actually there using regex and replace it
content = re.sub(r'// Support URLs containing bbox=\.\.\. within shared text.*?val parts = bboxStr\.split\(\",\"\)', target, content, flags=re.DOTALL)

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
