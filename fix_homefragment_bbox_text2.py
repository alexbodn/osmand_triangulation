import re

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'r') as f:
    content = f.read()

# Fix the double backslash inside the Kotlin string which caused string termination syntax errors
content = content.replace('substringBefore("\\\\n")', 'substringBefore("\\n")')
content = content.replace('substringBefore("\\n")', 'substringBefore("\\n")') # It's probably `substringBefore("\n")` that's needed.

with open('app/app/src/main/java/com/example/triangulation/ui/HomeFragment.kt', 'w') as f:
    f.write(content)
