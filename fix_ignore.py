import re

with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'r') as f:
    content = f.read()

old_filter = ".filter(w => !['There', 'are', 'out', 'of', 'seen', 'whitelisted', 'players', 'and', 'the'].includes(w))"
new_filter = ".filter(w => !['there', 'are', 'out', 'of', 'seen', 'whitelisted', 'whitelist', 'players', 'size', 'and', 'the'].includes(w.toLowerCase()))"

content = content.replace(old_filter, new_filter)

with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'w') as f:
    f.write(content)
