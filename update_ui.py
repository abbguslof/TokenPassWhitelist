import re

with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'r') as f:
    content = f.read()

# 1. Add searchQuery and reactive users list
reactive_code = """
	let whitelistSearchQuery = '';
	
	$: parsedWhitelistUsers = whitelistOutput
		.join(' ')
		.replace(/,/g, ' ')
		.split(/\\s+/)
		.map(w => w.trim())
		.filter(w => /^[a-zA-Z0-9_]{3,16}$/.test(w))
		.filter(w => !['There', 'are', 'out', 'of', 'seen', 'whitelisted', 'players', 'and', 'the'].includes(w))
		.filter((v, i, a) => a.indexOf(v) === i)
		.filter(w => w.toLowerCase().includes(whitelistSearchQuery.toLowerCase()));
"""
content = content.replace("let whitelistOutput: string[] = [];", "let whitelistOutput: string[] = [];" + reactive_code)

# 2. Update the UI for whitelist
old_ui = """
					<div class="console-output">
						{#each whitelistOutput as line}
							<div>{line}</div>
						{/each}
						{#if whitelistOutput.length === 0}
							<div>No whitelist output received.</div>
						{/if}
					</div>
					
					<h3>Manage Existing Players</h3>
					<p class="help-text">Type a username above to manually remove if it's not easily clickable below.</p>
					<form class="inline-form" on:submit|preventDefault={() => removeWhitelist(newWhitelistUser)}>
						<input type="text" placeholder="Remove username..." bind:value={newWhitelistUser} required />
						<button type="submit" class="delete-btn">Remove Player</button>
					</form>
"""

new_ui = """
					<h3>Manage Whitelisted Players</h3>
					<div class="inline-form">
						<input type="text" placeholder="Search whitelisted players..." bind:value={whitelistSearchQuery} />
					</div>
					
					<table>
						<thead><tr><th>Username</th><th>Action</th></tr></thead>
						<tbody>
							{#each parsedWhitelistUsers as u}
								<tr>
									<td>{u}</td>
									<td><button class="delete-btn" on:click={() => removeWhitelist(u)}>Remove</button></td>
								</tr>
							{/each}
							{#if parsedWhitelistUsers.length === 0}
								<tr><td colspan="2">No players found matching your search.</td></tr>
							{/if}
						</tbody>
					</table>
					
					<details style="margin-top: 1rem; color: #666;">
						<summary>View raw console output</summary>
						<div class="console-output" style="margin-top: 0.5rem;">
							{#each whitelistOutput as line}
								<div>{line}</div>
							{/each}
						</div>
					</details>
"""
content = content.replace(old_ui, new_ui)

with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'w') as f:
    f.write(content)

