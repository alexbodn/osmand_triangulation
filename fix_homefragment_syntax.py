import re

with open('/tmp/HomeFragment.kt', 'r') as f:
    content = f.read()

content = content.replace('''    private fun libraryManager = com.example.triangulation.data.LocationLibraryManager(requireContext())
            loadState() {''', '    private fun loadState() {')
content = content.replace('intent = Intent()', 'requireActivity().intent.replaceExtras(Bundle())')

with open('/tmp/HomeFragment.kt', 'w') as f:
    f.write(content)
