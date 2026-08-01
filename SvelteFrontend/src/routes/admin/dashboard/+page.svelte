<script lang="ts">
	import { onMount, tick } from 'svelte';
	import { goto } from '$app/navigation';
	import { Network } from 'vis-network';

	/**
	 * State Management
	 * password: Used for all proxy requests to the backend
	 * activeTab: Controls which dashboard view is rendered
	 */
	let password = '';
	let activeTab = 'tree';
	
	let invites: any[] = [];
	let permanentLinks: any[] = [];
	let whitelistOutput: string[] = [];
	let whitelistSearchQuery = '';
	let selectedNode: any = null;
	
	$: allWhitelistUsers = whitelistOutput
		.join(' ')
		.replace(/,/g, ' ')
		.split(/\s+/)
		.map(w => w.trim())
		.filter(w => /^[a-zA-Z0-9_.*-]{3,24}$/.test(w))
		.filter(w => !['there', 'are', 'out', 'of', 'seen', 'whitelisted', 'whitelist', 'players', 'size', 'and', 'the'].includes(w.toLowerCase()))
		.filter((v, i, a) => a.indexOf(v) === i);

	$: parsedWhitelistUsers = allWhitelistUsers
		.filter(w => w.toLowerCase().includes(whitelistSearchQuery.toLowerCase()));

	let newWhitelistUser = '';
	let players: any[] = [];
	
	let loading = true;
	let error = '';

	let newInviteUser = '';
	let newInviteCreator = '';
	let inviteResult = '';
	
	let permCreator = '';
	let permPassword = '';
	let permResult = '';

	let networkContainer: HTMLElement;
	let network: any;

	onMount(async () => {
		const savedTab = localStorage.getItem('activeTab');
		if (savedTab) activeTab = savedTab;
		
		password = localStorage.getItem('adminPassword') || '';
		if (!password) {
			goto('/admin');
			return;
		}
		await fetchData();
	});

	/**
	 * Core API utility for the dashboard. 
	 * Proxies requests through the internal Svelte endpoint to hide the 
	 * admin password from network inspectors and handle CORS internally.
	 */
	async function apiCall(action: string, payload: any = {}) {
		const res = await fetch('/admin/dashboard/api', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ action, password, payload })
		});
		const data = await res.json();
		if (!res.ok) {
			if (res.status === 401) {
				localStorage.removeItem('adminPassword');
				goto('/admin');
			}
			throw new Error(data.message || 'API Error');
		}
		return data;
	}

	async function fetchData() {
		try {
			loading = true;
			const invData = await apiCall('invites');
			const plData = await apiCall('players');
const wlData = await apiCall('whitelist-list');
			const permData = await apiCall('permanent-links');
			invites = invData.invites;
			players = plData.players;
			whitelistOutput = wlData.output;
			permanentLinks = permData.permanentLinks;
			
			if (activeTab === 'tree') await renderTree();
		} catch (e: any) {
			error = e.message;
		} finally {
			loading = false;
		}
	}

	/**
	 * Renders the vis-network graph for the invite tree.
	 * Constructs nodes and edges based on inviter-target relationships.
	 */
	async function renderTree() {
		await tick();
		if (!networkContainer) return;

		const nodes = new Map();
		const edges: any[] = [];
		const whitelistSet = new Set(allWhitelistUsers.map(u => u.toLowerCase()));
		
		nodes.set('ROOT', { id: 'ROOT', label: 'Server Console', shape: 'star', color: '#ffd700' });

		invites.forEach(inv => {
			const creatorId = inv.inviterName || 'ROOT';
			if (!nodes.has(creatorId)) {
				nodes.set(creatorId, { id: creatorId, label: creatorId, shape: 'box', color: '#add8e6' });
			}
			
			if (inv.targetName) {
				const isWhitelisted = whitelistSet.has(inv.targetName.toLowerCase());
				const nodeColor = isWhitelisted ? '#90ee90' : '#ff6b6b';
				const label = isWhitelisted ? inv.targetName : `❌ ${inv.targetName}`;
				nodes.set(inv.targetName, {
					id: inv.targetName,
					label,
					shape: 'box',
					color: nodeColor,
					font: isWhitelisted ? {} : { color: '#fff' },
					_token: inv.token,
					_type: 'claimed'
				});
				edges.push({ from: creatorId, to: inv.targetName, arrows: 'to' });
			} else {
				nodes.set(inv.token, {
					id: inv.token,
					label: 'Pending Invite',
					shape: 'ellipse',
					color: '#ffcccb',
					_token: inv.token,
					_type: 'pending'
				});
				edges.push({ from: creatorId, to: inv.token, arrows: 'to', dashes: true });
			}
		});

		const data = {
			nodes: Array.from(nodes.values()),
			edges: edges
		};

		const options = {
			layout: { hierarchical: { direction: 'UD', sortMethod: 'directed' } },
			physics: false,
			interaction: { selectConnectedEdges: false }
		};

		if (network) network.destroy();
		network = new Network(networkContainer, data, options);

		// Store nodes map for selection lookup
		const nodesMap = nodes;

		network.on('selectNode', (params: any) => {
			const nodeId = params.nodes[0];
			const node = nodesMap.get(nodeId);
			if (node && node._token) {
				selectedNode = { id: nodeId, token: node._token, type: node._type };
			} else {
				selectedNode = null;
			}
		});

		network.on('deselectNode', () => {
			selectedNode = null;
		});
	}

	
	async function deletePermanentLink(id: string) {
		if (confirm('Are you sure you want to delete this permanent link?')) {
			try {
				await apiCall('delete-permanent-link', { id });
				await fetchData();
			} catch (e: any) { alert(e.message); }
		}
	}

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

	function switchTab(tab: string) {
		activeTab = tab;
		localStorage.setItem('activeTab', tab);
		if (tab === 'tree') renderTree();
	}

	async function createInvite() {
		try {
			const data = await apiCall('invite-admin', { inviterName: newInviteCreator });
			inviteResult = window.location.origin + '/invite/' + data.token;
			newInviteUser = '';
			await fetchData();
		} catch (e: any) {
			alert(e.message);
		}
	}

async function createPermanentLink() {
		if (!confirm('Are you sure you want to generate a permanent link? Anyone with this link (and password, if set) can bypass the whitelist instantly.')) return;
		try {
			const data = await apiCall('permanent-link', { 
				creatorName: permCreator || 'Admin', 
				passwordHash: permPassword || null 
			});
			permResult = window.location.origin + '/public-invite/' + data.id;
			permPassword = '';
			await fetchData();
		} catch (e: any) {
			alert(e.message);
		}
	}
</script>

<svelte:head>
	<link rel="preconnect" href="https://fonts.googleapis.com">
	<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
	<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
</svelte:head>

<main class="dashboard">
	<aside class="sidebar">
		<div class="sidebar-header">
			<div class="logo">🛡️</div>
			<h2>TokenPass</h2>
			<span class="version-tag">Admin</span>
		</div>
		<nav>
			<button class:active={activeTab === 'tree'} on:click={() => switchTab('tree')}>
				<span class="nav-icon">🌳</span> Invite Tree
			</button>
			<button class:active={activeTab === 'active'} on:click={() => switchTab('active')}>
				<span class="nav-icon">🎫</span> Active Invites
			</button>
			<button class:active={activeTab === 'whitelist'} on:click={() => switchTab('whitelist')}>
				<span class="nav-icon">📋</span> Whitelist
			</button>
			<button class:active={activeTab === 'generate'} on:click={() => switchTab('generate')}>
				<span class="nav-icon">🎟️</span> Generate Invite
			</button>
			<button class:active={activeTab === 'permanent'} on:click={() => switchTab('permanent')}>
				<span class="nav-icon">🔗</span> Permanent Links
			</button>
		</nav>
		<div class="sidebar-footer">
			<button class="logout-btn" on:click={() => { localStorage.removeItem('adminPassword'); goto('/admin'); }}>
				🚪 Logout
			</button>
		</div>
	</aside>

	<section class="content">
		{#if loading}
			<div class="loading-state">
				<div class="spinner"></div>
				<p>Loading dashboard...</p>
			</div>
		{:else if error}
			<div class="card error-card"><p class="error">{error}</p></div>
		{:else}
			{#if activeTab === 'tree'}
				<div class="card">
					<h3>Invite Tree Visualization</h3>
					<p class="tree-legend">
						<span class="legend-item"><span class="legend-dot" style="background:#90ee90"></span> Whitelisted</span>
						<span class="legend-item"><span class="legend-dot" style="background:#ff6b6b"></span> Removed</span>
						<span class="legend-item"><span class="legend-dot" style="background:#ffcccb; border-radius:50%"></span> Pending</span>
						<span class="legend-item"><span class="legend-dot" style="background:#add8e6"></span> Inviter</span>
					</p>
					{#if selectedNode}
						<div class="tree-actions">
							<span>Selected: <strong>{selectedNode.id}</strong> ({selectedNode.type})</span>
							<button class="delete-btn" on:click={async () => {
								if (confirm(`Delete invite record for "${selectedNode.id}"? This removes the entry from the tree.`)) {
									try {
										await apiCall('delete-invite', { token: selectedNode.token });
										selectedNode = null;
										await fetchData();
									} catch (e) { alert(e.message); }
								}
							}}>🗑️ Remove from Tree</button>
						</div>
					{/if}
					<div bind:this={networkContainer} class="network-canvas"></div>
				</div>
			{/if}

			
{#if activeTab === 'active'}
				<div class="card">
					<h3>Permanent Links</h3>
					<div class="table-responsive">
					<table>
						<thead><tr><th>ID</th><th>Creator</th><th>Password</th><th>Uses</th><th>Created</th><th>Actions</th></tr></thead>
						<tbody>
							{#each permanentLinks as p}
								<tr>
									<td class="mono">{p.id.substring(0, 8)}…</td>
									<td>{p.creatorName}</td>
									<td>{#if p.hasPassword}<span class="badge badge-yes">Yes</span>{:else}<span class="badge badge-no">No</span>{/if}</td>
									<td><span class="badge badge-count">{p.uses}</span></td>
									<td>{new Date(p.createdAt).toLocaleDateString()}</td>
									<td class="action-cell">
										<button class="btn-sm btn-outline" on:click={() => { navigator.clipboard.writeText(window.location.origin + '/public-invite/' + p.id); }}>📋 Copy</button>
										<button class="btn-sm btn-danger" on:click={() => deletePermanentLink(p.id)}>Delete</button>
									</td>
								</tr>
							{/each}
							{#if permanentLinks.length === 0}
								<tr><td colspan="6">No permanent links created.</td></tr>
							{/if}
						</tbody>
					</table>
					</div>
				</div>

				<div class="card">
					<h3>Active Invites (Unclaimed)</h3>
					<div class="table-responsive">
					<table>
						<thead><tr><th>Token</th><th>Inviter</th><th>Date</th><th>Action</th></tr></thead>
						<tbody>
							{#each invites.filter(i => !i.targetName) as i}
								<tr>
									<td class="mono">{i.token.substring(0, 8)}…</td>
									<td>
										{#if i.inviterName && i.inviterName.startsWith('[Admin]')}
											<span class="admin-badge">Admin</span> {i.inviterName.replace('[Admin] ', '')}
										{:else}
											{i.inviterName}
										{/if}
									</td>
									<td>{new Date(i.createdAt).toLocaleString()}</td>
									<td class="action-cell"><button class="btn-sm btn-danger" on:click={() => deleteInvite(i.token)}>Delete</button></td>
								</tr>
							{/each}
							{#if invites.filter(i => !i.targetName).length === 0}
								<tr><td colspan="4">No active invites.</td></tr>
							{/if}
						</tbody>
					</table>
					</div>
				</div>
			{/if}

			{#if activeTab === 'whitelist'}
				<div class="card">
					<h3>Currently Online Players (Proxy)</h3>
					<div class="table-responsive">
					<table>
						<thead><tr><th>UUID</th><th>Username</th></tr></thead>
						<tbody>
							{#each players as p}
								<tr><td>{p.uuid}</td><td>{p.username}</td></tr>
							{/each}
							{#if players.length === 0}
								<tr><td colspan="2">No players online.</td></tr>
							{/if}
						</tbody>
					</table>
					</div>
				</div>
				
				<div class="card">
					<h3>Manage Whitelisted Players ({parsedWhitelistUsers.length} total)</h3>
					<div class="actions-row">
						<form class="inline-form" on:submit|preventDefault={addWhitelist}>
							<input type="text" placeholder="New username..." bind:value={newWhitelistUser} required />
							<button type="submit">Add Player</button>
						</form>
						<input type="text" class="search-bar" placeholder="Search players..." bind:value={whitelistSearchQuery} />
					</div>
					
					<div class="table-responsive">
					<table>
						<thead><tr><th>Username</th><th>Action</th></tr></thead>
						<tbody>
							{#each parsedWhitelistUsers as u}
								<tr>
									<td>{u}</td>
									<td class="action-cell"><button class="btn-sm btn-danger" on:click={() => removeWhitelist(u)}>Remove</button></td>
								</tr>
							{/each}
							{#if parsedWhitelistUsers.length === 0}
								<tr><td colspan="2">No players found matching your search.</td></tr>
							{/if}
						</tbody>
					</table>
					</div>
					
					<details style="margin-top: 1rem; color: #666;">
						<summary>View raw console output</summary>
						<div class="console-output" style="margin-top: 0.5rem;">
							{#each whitelistOutput as line}
								<div>{line}</div>
							{/each}
						</div>
					</details>
				</div>

			{/if}

			{#if activeTab === 'generate'}
				<div class="card">
					<h3>Generate Single-Use Invite</h3>
					<form on:submit|preventDefault={createInvite}>
						
						<input type="text" placeholder="Inviter Name (optional)" bind:value={newInviteCreator} />
						<button type="submit">Generate</button>
					</form>
					{#if inviteResult}
						<div class="result-box">
							<p>Link created: <a href={inviteResult} target="_blank">{inviteResult}</a></p>
						</div>
					{/if}
				</div>
			{/if}

			{#if activeTab === 'permanent'}
				<div class="card">
					<h3>Generate Permanent Public Link</h3>
					<p class="warning">⚠️ Warning: Permanent links can be abused if leaked without a password. Consider adding a password to restrict usage.</p>
					<form on:submit|preventDefault={createPermanentLink}>
						<input type="text" placeholder="Creator Name (e.g. Discord)" bind:value={permCreator} required />
						<input type="text" placeholder="Optional Password" bind:value={permPassword} />
						<button type="submit">Generate Permanent Link</button>
					</form>
					{#if permResult}
						<div class="result-box">
							<p>Permanent Link created: <a href={permResult} target="_blank">{permResult}</a></p>
						</div>
					{/if}
				</div>
			{/if}
		{/if}
	</section>
</main>

<style>
	:global(body) { margin: 0; padding: 0; font-family: 'Inter', system-ui, -apple-system, sans-serif; }
	.dashboard {
		display: flex;
		height: 100vh;
		background: #f0f2f5;
	}

	/* Sidebar */
	.sidebar {
		width: 260px;
		background: linear-gradient(180deg, #1a2332 0%, #2c3e50 100%);
		color: white;
		padding: 0;
		display: flex;
		flex-direction: column;
		box-shadow: 2px 0 12px rgba(0,0,0,0.1);
	}
	.sidebar-header {
		padding: 1.75rem 1.5rem;
		text-align: center;
		border-bottom: 1px solid rgba(255,255,255,0.08);
	}
	.logo { font-size: 2.5rem; margin-bottom: 0.5rem; }
	.sidebar-header h2 { margin: 0; font-size: 1.3rem; font-weight: 700; letter-spacing: 0.5px; }
	.version-tag {
		display: inline-block;
		margin-top: 0.4rem;
		padding: 2px 10px;
		background: rgba(255,255,255,0.1);
		border-radius: 12px;
		font-size: 0.75rem;
		letter-spacing: 1px;
		text-transform: uppercase;
		color: rgba(255,255,255,0.6);
	}
	.sidebar nav {
		display: flex;
		flex-direction: column;
		padding: 1rem 0.75rem;
		gap: 0.25rem;
		flex: 1;
	}
	.sidebar button {
		background: transparent;
		color: rgba(255,255,255,0.7);
		border: none;
		padding: 0.7rem 1rem;
		text-align: left;
		font-size: 0.9rem;
		font-weight: 500;
		cursor: pointer;
		border-radius: 8px;
		transition: all 0.15s ease;
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}
	.sidebar button:hover { background: rgba(255,255,255,0.08); color: #fff; }
	.sidebar button.active { background: #007cba; color: #fff; box-shadow: 0 2px 8px rgba(0,124,186,0.3); }
	.nav-icon { font-size: 1.1rem; width: 1.5rem; text-align: center; }
	.sidebar-footer {
		padding: 1rem 0.75rem;
		border-top: 1px solid rgba(255,255,255,0.08);
	}
	.logout-btn {
		width: 100%;
		background: rgba(255,255,255,0.05) !important;
		color: rgba(255,255,255,0.5) !important;
		justify-content: center;
	}
	.logout-btn:hover { background: rgba(211,47,47,0.15) !important; color: #ff6b6b !important; }

	/* Content Area */
	.content {
		flex: 1;
		padding: 2rem 2.5rem;
		overflow-y: auto;
	}
	.card {
		background: white;
		border-radius: 12px;
		padding: 1.75rem;
		box-shadow: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
		margin-bottom: 1.5rem;
		border: 1px solid rgba(0,0,0,0.04);
	}
	.card h3 {
		margin: 0 0 1rem 0;
		font-size: 1.15rem;
		font-weight: 600;
		color: #1a2332;
	}

	/* Loading */
	.loading-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		height: 60vh;
		gap: 1rem;
		color: #999;
	}
	.spinner {
		width: 36px;
		height: 36px;
		border: 3px solid #e0e0e0;
		border-top: 3px solid #007cba;
		border-radius: 50%;
		animation: spin 0.8s linear infinite;
	}
	@keyframes spin { 100% { transform: rotate(360deg); } }
	.error-card { border-left: 4px solid #d32f2f; }
	.error { color: #d32f2f; margin: 0; }

	/* Network Canvas */
	.full-height { height: calc(100vh - 8rem); display: flex; flex-direction: column; }
	.network-canvas {
		height: 600px;
		width: 100%;
		border: 1px solid #e8eaed;
		border-radius: 8px;
		margin-top: 0.75rem;
		background: #fafbfc;
	}

	/* Tables */
	.table-responsive { overflow-x: auto; }
	table { width: 100%; border-collapse: collapse; margin-top: 0.75rem; }
	th, td { text-align: left; padding: 0.75rem 1rem; font-size: 0.9rem; }
	th {
		background: #f8f9fb;
		color: #5f6368;
		font-weight: 600;
		font-size: 0.8rem;
		text-transform: uppercase;
		letter-spacing: 0.5px;
		border-bottom: 2px solid #e8eaed;
	}
	td { border-bottom: 1px solid #f0f1f3; color: #3c4043; }
	tbody tr { transition: background 0.15s ease; }
	tbody tr:hover { background: #f8f9fb; }
	.mono { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 0.85rem; color: #666; }
	.action-cell { white-space: nowrap; }

	/* Forms */
	form { display: flex; flex-direction: column; gap: 1rem; max-width: 420px; }
	input {
		padding: 0.7rem 0.85rem;
		border: 1.5px solid #dde1e6;
		border-radius: 8px;
		font-size: 0.9rem;
		color: #1a2332;
		transition: border-color 0.15s, box-shadow 0.15s;
		font-family: inherit;
	}
	input:focus {
		outline: none;
		border-color: #007cba;
		box-shadow: 0 0 0 3px rgba(0,124,186,0.1);
	}
	input::placeholder { color: #aab0b8; }
	button[type="submit"] {
		background: #007cba;
		color: white;
		border: none;
		padding: 0.7rem 1.25rem;
		border-radius: 8px;
		cursor: pointer;
		font-weight: 600;
		font-size: 0.9rem;
		transition: all 0.15s ease;
		font-family: inherit;
	}
	button[type="submit"]:hover {
		background: #006da8;
		transform: translateY(-1px);
		box-shadow: 0 4px 12px rgba(0,124,186,0.25);
	}

	/* Utility */
	.warning {
		color: #856404;
		background: #fff8e1;
		padding: 0.85rem 1rem;
		border-radius: 8px;
		border: 1px solid #ffe082;
		font-size: 0.9rem;
		line-height: 1.5;
	}
	.result-box {
		margin-top: 1rem;
		padding: 1rem 1.25rem;
		background: #e8f5e9;
		color: #2e7d32;
		border-radius: 8px;
		word-break: break-all;
		font-size: 0.9rem;
		border: 1px solid #c8e6c9;
	}
	.result-box a { color: #1b5e20; font-weight: 600; }

	/* Badges */
	.admin-badge {
		background: #ff9800;
		color: white;
		padding: 2px 8px;
		border-radius: 4px;
		font-size: 0.75rem;
		font-weight: 600;
		text-transform: uppercase;
		letter-spacing: 0.5px;
	}
	.badge {
		padding: 3px 10px;
		border-radius: 12px;
		font-size: 0.8rem;
		font-weight: 600;
	}
	.badge-yes { background: #e8f5e9; color: #2e7d32; }
	.badge-no { background: #f5f5f5; color: #999; }
	.badge-count { background: #e3f2fd; color: #1565c0; }

	/* Small Buttons */
	.btn-sm {
		padding: 0.35rem 0.75rem;
		border-radius: 6px;
		font-size: 0.8rem;
		font-weight: 500;
		cursor: pointer;
		border: none;
		transition: all 0.15s ease;
		font-family: inherit;
	}
	.btn-danger {
		background: #ffebee;
		color: #c62828;
	}
	.btn-danger:hover { background: #d32f2f; color: white; }
	.btn-outline {
		background: #f5f5f5;
		color: #555;
		border: 1px solid #ddd;
	}
	.btn-outline:hover { background: #e0e0e0; }

	/* Legacy delete-btn for tree actions */
	.delete-btn {
		background: #ffebee !important;
		color: #c62828;
		border: none;
		padding: 0.45rem 1rem;
		border-radius: 6px;
		cursor: pointer;
		transition: all 0.15s ease;
		font-size: 0.85rem;
		font-weight: 500;
		font-family: inherit;
	}
	.delete-btn:hover { background: #d32f2f !important; color: white; }

	/* Action Row */
	.actions-row { display: flex; gap: 1rem; align-items: center; margin-bottom: 1rem; flex-wrap: wrap; justify-content: space-between; }
	.actions-row .inline-form { margin-bottom: 0; }
	.search-bar {
		padding: 0.7rem 0.85rem;
		border: 1.5px solid #dde1e6;
		border-radius: 8px;
		flex: 1;
		min-width: 200px;
		max-width: 300px;
		font-family: inherit;
		font-size: 0.9rem;
	}
	.inline-form { flex-direction: row; align-items: center; margin-bottom: 1rem; max-width: none; gap: 0.5rem; }
	.inline-form input { flex: 1; max-width: 300px; }
	.console-output {
		background: #1e1e2e;
		color: #cdd6f4;
		padding: 1rem;
		border-radius: 8px;
		font-family: 'SF Mono', 'Fira Code', monospace;
		font-size: 0.85rem;
		max-height: 200px;
		overflow-y: auto;
		margin-bottom: 1.5rem;
		line-height: 1.6;
	}
	.help-text { font-size: 0.85rem; color: #888; margin-top: -0.5rem; margin-bottom: 1rem; }

	/* Tree Legend & Actions */
	.tree-legend {
		display: flex;
		gap: 1.25rem;
		flex-wrap: wrap;
		margin-bottom: 0.75rem;
		font-size: 0.85rem;
		color: #666;
	}
	.legend-item { display: flex; align-items: center; gap: 0.4rem; }
	.legend-dot { display: inline-block; width: 12px; height: 12px; border-radius: 3px; border: 1px solid rgba(0,0,0,0.1); }
	.tree-actions {
		display: flex;
		align-items: center;
		gap: 1rem;
		padding: 0.65rem 1rem;
		background: #f0f4f8;
		border-radius: 8px;
		margin-bottom: 0.5rem;
		flex-wrap: wrap;
		border: 1px solid #e0e7ee;
	}
	.tree-actions span { font-size: 0.9rem; color: #3c4043; }

	/* Responsive */
	@media (max-width: 768px) {
		.dashboard {
			flex-direction: column;
			height: auto;
			min-height: 100vh;
		}
		.sidebar {
			width: 100%;
			flex-direction: row;
			align-items: center;
		}
		.sidebar-header { display: none; }
		.sidebar-footer { display: none; }
		.sidebar nav {
			flex-direction: row;
			padding: 0.5rem;
			gap: 0.25rem;
			overflow-x: auto;
		}
		.sidebar button {
			flex-shrink: 0;
			text-align: center;
			font-size: 0.8rem;
			padding: 0.5rem 0.75rem;
		}
		.nav-icon { display: none; }
		.content { padding: 1rem; }
		.card { padding: 1.25rem; }
		.full-height { height: 50vh; }
	}

</style>
