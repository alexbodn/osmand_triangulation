import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

# Fix the regex pattern syntax in Kotlin
content = content.replace('val regex = Regex("[0-9.,-]+")', 'val regex = Regex("[0-9.,-]+")')
content = content.replace('val regex = Regex("[0-9\.,-]+")', 'val regex = Regex("[0-9.,-]+")')
# Let's just remove the dot escape altogether and use a simpler regex or split mechanism
content = re.sub(r'val regex = Regex\(".*?"\)', 'val regex = Regex("[0-9.,-]+")', content)


with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
