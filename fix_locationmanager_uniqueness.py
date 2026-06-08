import re

with open('app/app/src/main/java/com/example/triangulation/data/LocationLibraryManager.kt', 'r') as f:
    content = f.read()

# Replace addLocation
add_logic = """
    fun addLocation(location: LibraryLocation) {
        val current = getLocations().toMutableList()
        // Replace if exists with exact same lat/lon AND desc
        val index = current.indexOfFirst { it.lat == location.lat && it.lon == location.lon && it.desc == location.desc }
        if (index != -1) {
            current[index] = location
        } else {
            current.add(location)
        }
        saveLocations(current)
    }
"""
content = re.sub(r'    fun addLocation.*?saveLocations\(current\)\n    \}', add_logic.strip(), content, flags=re.DOTALL)

# Replace removeLocation
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

# Replace isLocationInLibrary
is_logic = """
    fun isLocationInLibrary(lat: Double, lon: Double, desc: String? = null): LibraryLocation? {
        val current = getLocations()
        return if (desc != null) {
            current.firstOrNull { it.lat == lat && it.lon == lon && it.desc == desc }
        } else {
            // When querying just by coordinates (e.g. from Home tab active locations),
            // return the first match at that exact spot if one exists.
            current.firstOrNull { it.lat == lat && it.lon == lon }
        }
    }
"""
content = re.sub(r'    fun isLocationInLibrary.*?\}\n\}', is_logic.strip() + '\n}', content, flags=re.DOTALL)

with open('app/app/src/main/java/com/example/triangulation/data/LocationLibraryManager.kt', 'w') as f:
    f.write(content)
