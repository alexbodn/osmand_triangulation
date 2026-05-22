import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

# Replace the overly specific string contains check with something looser that ignores spacing
new_check = """
                if (sharedText.contains("\\"FeatureCollection\\"") || sharedText.contains("\\"Feature\\"")) {
"""
content = re.sub(r'                if \(sharedText\.contains\("\\"type\\": \\"FeatureCollection\\""\) \|\| sharedText\.contains\("\\"type\\": \\"Feature\\""\)\) \{', new_check, content)

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
