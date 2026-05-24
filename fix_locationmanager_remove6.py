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

content = content.replace("""    fun removeLocation(lat: Double, lon: Double) {
        val current = getLocations().toMutableList()
        val index = current.indexOfFirst { Math.abs(it.lat - lat) < 0.00001 && Math.abs(it.lon - lon) < 0.00001 }
        if (index != -1) {
            current.removeAt(index)
            saveLocations(current)
        }
    }""", remove_logic)

with open('app/app/src/main/java/com/example/triangulation/data/LocationLibraryManager.kt', 'w') as f:
    f.write(content)
