import re

with open('app/app/src/main/java/com/example/triangulation/ui/LocationsFragment.kt', 'r') as f:
    content = f.read()

content = content.replace('libraryManager.removeLocation(loc.lat, loc.lon)', 'libraryManager.removeLocation(loc.lat, loc.lon, loc.desc)')

with open('app/app/src/main/java/com/example/triangulation/ui/LocationsFragment.kt', 'w') as f:
    f.write(content)
