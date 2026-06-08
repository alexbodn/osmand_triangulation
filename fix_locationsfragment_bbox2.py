import re

with open('app/app/src/main/java/com/example/triangulation/ui/LocationsFragment.kt', 'r') as f:
    content = f.read()

# Make sure we're getting the right BBox bounds.
# BBox format is usually minLon, minLat, maxLon, maxLat
# The filter check is:
# it.lon in minLon..maxLon && it.lat in minLat..maxLat

# It looks correct. I will just verify that the parsing didn't mangle the bbox somehow.
