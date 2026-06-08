import re

with open('app/app/src/main/java/com/example/triangulation/data/LocationLibraryManager.kt', 'r') as f:
    content = f.read()

remove_logic = """
    fun removeLocation(lat: Double, lon: Double, desc: String?) {
        val current = getLocations().toMutableList()
        val index = current.indexOfFirst { it.lat == lat && it.lon == lon && it.desc == desc }
        if (index != -1) {
            current.removeAt(index)
            saveLocations(current)
        }
    }
"""

content = re.sub(r'    fun removeLocation.*?saveLocations\(current\)\n    \}', remove_logic.strip(), content, flags=re.DOTALL)

with open('app/app/src/main/java/com/example/triangulation/data/LocationLibraryManager.kt', 'w') as f:
    f.write(content)
