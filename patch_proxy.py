import re

with open('SvelteFrontend/src/routes/admin/dashboard/api/+server.ts', 'r') as f:
    content = f.read()

replacement = """	if (action === 'whitelist-list' || action === 'invites' || action === 'players' || action === 'permanent-links') {
		const res = await fetch(`http://localhost:5000/api/${action}`, {
			headers: { 'X-Admin-Password': password }
		});
		return new Response(res.body, { status: res.status });
	}"""

content = re.sub(r"if \(action === 'whitelist-list' \|\| action === 'invites' \|\| action === 'players'\) \{[^\}]+return new Response\(res\.body, \{ status: res\.status \}\);\n\t\}", replacement, content)

with open('SvelteFrontend/src/routes/admin/dashboard/api/+server.ts', 'w') as f:
    f.write(content)
