import re

with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'r') as f:
    content = f.read()

# 1. State vars
content = content.replace("let invites: any[] = [];", "let invites: any[] = [];\n\tlet whitelistOutput: string[] = [];\n\tlet newWhitelistUser = '';")

# 2. Fetch data
content = content.replace("const plData = await apiCall('players');", "const plData = await apiCall('players');\n\t\t\tconst wlData = await apiCall('whitelist-list');")
content = content.replace("players = plData.players;", "players = plData.players;\n\t\t\twhitelistOutput = wlData.output;")

# 3. New API functions
new_funcs = """
	async function deleteInvite(token: string) {
		if (confirm('Are you sure you want to delete this invite?')) {
			try {
				await apiCall('delete-invite', { token });
				await fetchData();
			} catch (e: any) { alert(e.message); }
		}
	}

	async function removeWhitelist(username: string) {
		if (confirm('Are you sure you want to remove ' + username + ' from the whitelist?')) {
			try {
				await apiCall('remove-whitelist', { username });
				await fetchData();
			} catch (e: any) { alert(e.message); }
		}
	}

	async function addWhitelist() {
		if (!newWhitelistUser) return;
		try {
			await apiCall('add-whitelist', { username: newWhitelistUser });
			newWhitelistUser = '';
			await fetchData();
		} catch (e: any) { alert(e.message); }
	}
"""
content = content.replace("function switchTab(tab: string) {", new_funcs + "\n\tfunction switchTab(tab: string) {")

# 4. update createInvite
content = content.replace("inviteResult = data.link;", "inviteResult = window.location.origin + '/invite/' + data.token;")

# 5. Sidebar buttons
content = content.replace("🌳 Invite Tree</button>", "🌳 Invite Tree</button>\n\t\t\t<button class:active={activeTab === 'active'} on:click={() => switchTab('active')}>🎫 Active Invites</button>")

# 6. Replace Active Invites section
active_invites_html = """
			{#if activeTab === 'active'}
				<div class="card">
					<h3>Active Invites (Unclaimed)</h3>
					<table>
						<thead><tr><th>Token</th><th>Inviter</th><th>Date</th><th>Action</th></tr></thead>
						<tbody>
							{#each invites.filter(i => !i.targetName) as i}
								<tr>
									<td>{i.token}</td>
									<td>
										{#if i.inviterName && i.inviterName.startsWith('[Admin]')}
											<span class="admin-badge">Admin</span> {i.inviterName.replace('[Admin] ', '')}
										{:else}
											{i.inviterName}
										{/if}
									</td>
									<td>{new Date(i.createdAt).toLocaleString()}</td>
									<td><button class="delete-btn" on:click={() => deleteInvite(i.token)}>Delete</button></td>
								</tr>
							{/each}
							{#if invites.filter(i => !i.targetName).length === 0}
								<tr><td colspan="4">No active invites.</td></tr>
							{/if}
						</tbody>
					</table>
				</div>
			{/if}
"""
content = content.replace("{#if activeTab === 'whitelist'}", active_invites_html + "\n\t\t\t{#if activeTab === 'whitelist'}")

# 7. Update Whitelist section
whitelist_html = """
				<div class="card">
					<h3>Server Whitelist</h3>
					<form class="inline-form" on:submit|preventDefault={addWhitelist}>
						<input type="text" placeholder="Add username..." bind:value={newWhitelistUser} required />
						<button type="submit">Add to Whitelist</button>
					</form>
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
				</div>
"""
# We replace the entire Whitelist section, keeping the online players proxy part
content = re.sub(r'<div class="card">\s*<h3>Whitelisted via Invites</h3>.*?</div>', whitelist_html, content, flags=re.DOTALL)

# 8. Remove target username from generate invite
content = content.replace('<input type="text" placeholder="Target Username" bind:value={newInviteUser} required />', '')
content = content.replace("username: newInviteUser, inviterName: newInviteCreator || 'Admin'", "inviterName: newInviteCreator")

# 9. Add styles
styles = """
	.delete-btn { background: #d32f2f !important; }
	.delete-btn:hover { background: #b71c1c !important; }
	.admin-badge { background: #ff9800; color: white; padding: 2px 6px; border-radius: 4px; font-size: 0.8rem; font-weight: bold; }
	.inline-form { flex-direction: row; align-items: center; margin-bottom: 1rem; max-width: none; }
	.inline-form input { flex: 1; max-width: 300px; }
	.console-output { background: #1e1e1e; color: #d4d4d4; padding: 1rem; border-radius: 4px; font-family: monospace; max-height: 200px; overflow-y: auto; margin-bottom: 1.5rem; }
	.help-text { font-size: 0.9rem; color: #666; margin-top: -0.5rem; margin-bottom: 1rem; }
"""
content = content.replace("</style>", styles + "\n</style>")

with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'w') as f:
    f.write(content)

