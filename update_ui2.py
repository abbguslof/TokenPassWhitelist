import re

with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'r') as f:
    content = f.read()

# Replace the two blocks of HTML
old_html = """
					<h3>Server Whitelist</h3>
					<form class="inline-form" on:submit|preventDefault={addWhitelist}>
						<input type="text" placeholder="Add username..." bind:value={newWhitelistUser} required />
						<button type="submit">Add to Whitelist</button>
					</form>
					<h3>Manage Whitelisted Players</h3>
					<div class="inline-form">
						<input type="text" placeholder="Search whitelisted players..." bind:value={whitelistSearchQuery} />
					</div>
"""

new_html = """
					<h3>Manage Whitelisted Players ({parsedWhitelistUsers.length} total)</h3>
					<div class="actions-row">
						<form class="inline-form" on:submit|preventDefault={addWhitelist}>
							<input type="text" placeholder="New username..." bind:value={newWhitelistUser} required />
							<button type="submit">Add Player</button>
						</form>
						<input type="text" class="search-bar" placeholder="Search players..." bind:value={whitelistSearchQuery} />
					</div>
"""

content = content.replace(old_html.strip(), new_html.strip())

# Replace CSS for delete-btn
old_css = """	.delete-btn { background: #d32f2f !important; }
	.delete-btn:hover { background: #b71c1c !important; }"""

new_css = """	.delete-btn { background: #d32f2f !important; color: white; border: none; padding: 0.5rem 1rem; border-radius: 4px; cursor: pointer; transition: background 0.2s; font-size: 0.9rem; }
	.delete-btn:hover { background: #b71c1c !important; }
	.actions-row { display: flex; gap: 1rem; align-items: center; margin-bottom: 1rem; flex-wrap: wrap; justify-content: space-between; }
	.actions-row .inline-form { margin-bottom: 0; }
	.search-bar { padding: 0.75rem; border: 1px solid #ccc; border-radius: 4px; flex: 1; min-width: 200px; max-width: 300px; }"""

content = content.replace(old_css, new_css)

with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'w') as f:
    f.write(content)
