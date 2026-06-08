import re

with open('app/app/src/main/java/com/example/triangulation/data/LocationLibraryManager.kt', 'r') as f:
    content = f.read()

remove_logic = """    fun removeLocation(lat: Double, lon: Double, desc: String? = null) {
        val current = getLocations().toMutableList()
        val index = if (desc != null) {
            current.indexOfFirst { it.lat == lat && it.lon == lon && it.desc == desc }
        } else {
            current.indexOfFirst { it.lat == lat && it.lon == lon }
        }

        if (index != -1) {
            current.removeAt(index)
            saveLocations(current)
        }
    }"""

content = re.sub(r'    fun removeLocation.*?saveLocations\(current\)\n    \}', remove_logic, content, flags=re.DOTALL)

with open('app/app/src/main/java/com/example/triangulation/data/LocationLibraryManager.kt', 'w') as f:
    f.write(content)
