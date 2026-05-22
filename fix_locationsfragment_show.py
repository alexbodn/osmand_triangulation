import re

with open('app/app/src/main/java/com/example/triangulation/ui/LocationsFragment.kt', 'r') as f:
    content = f.read()

# Fix unbindService timing
content = content.replace('osmandHelper.unbindService()', '')

new_ondestroy = """
    override fun onDestroy() {
        super.onDestroy()
        osmandHelper.unbindService()
    }
}
"""
content = content.replace('override fun onContextMenuButtonClicked(buttonId: Int, pointId: String?, layerId: String?) {}\n}', 'override fun onContextMenuButtonClicked(buttonId: Int, pointId: String?, layerId: String?) {}\n' + new_ondestroy)

with open('app/app/src/main/java/com/example/triangulation/ui/LocationsFragment.kt', 'w') as f:
    f.write(content)
