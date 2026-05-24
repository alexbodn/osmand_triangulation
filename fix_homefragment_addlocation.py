import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

# Update the isLocationInLibrary check in the GeoJSON importer loop
content = content.replace(
    'if (libraryManager.isLocationInLibrary(loc.lat, loc.lon) == null) {',
    'if (libraryManager.isLocationInLibrary(loc.lat, loc.lon, loc.desc) == null) {'
)

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
