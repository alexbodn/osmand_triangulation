import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

content = content.replace('val regex = Regex("[0-9\\\\.,-]+")', 'val regex = Regex("[0-9.,-]+")')

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
