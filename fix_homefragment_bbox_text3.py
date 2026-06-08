import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

# Fix the broken line
content = content.replace('substringBefore("&").substringBefore("\n").substringBefore(" ")', 'substringBefore("&").substringBefore("\\n").substringBefore(" ")')

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
