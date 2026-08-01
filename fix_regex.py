import re

# 1. Backend
with open('VelocityPlugin/src/main/java/dev/tokenpass/tokenpasswhitelist/InternalHttpServer.java', 'r') as f:
    content = f.read()

content = content.replace('String username = json.get("username").getAsString();', 'String username = json.get("username").getAsString().trim();')
content = content.replace('^[a-zA-Z0-9_]{3,16}$', '^[a-zA-Z0-9_.*-]{3,24}$')

with open('VelocityPlugin/src/main/java/dev/tokenpass/tokenpasswhitelist/InternalHttpServer.java', 'w') as f:
    f.write(content)


# 2. Frontend Dashboard
with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'r') as f:
    content = f.read()

content = content.replace('/^[a-zA-Z0-9_]{3,16}$/', '/^[a-zA-Z0-9_.*-]{3,24}$/')

with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'w') as f:
    f.write(content)


# 3. Frontend Invites
for path in ['SvelteFrontend/src/routes/public-invite/[id]/+page.svelte', 'SvelteFrontend/src/routes/invite/[token]/+page.svelte']:
    with open(path, 'r') as f:
        content = f.read()
    
    content = content.replace('pattern="[a-zA-Z0-9_]{3,16}"', 'pattern="\\s*[a-zA-Z0-9_.*-]{3,24}\\s*"')
    
    with open(path, 'w') as f:
        f.write(content)

