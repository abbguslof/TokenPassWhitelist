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
	let whitelistOutput: string[] = [];
	let whitelistSearchQuery = '';
	
	$: parsedWhitelistUsers = whitelistOutput
		.join(' ')
		.replace(/,/g, ' ')
		.split(/\s+/)
		.map(w => w.trim())
		.filter(w => /^[a-zA-Z0-9_]{3,16}$/.test(w))
		.filter(w => !['there', 'are', 'out', 'of', 'seen', 'whitelisted', 'whitelist', 'players', 'size', 'and', 'the'].includes(w.toLowerCase()))
		.filter((v, i, a) => a.indexOf(v) === i)
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
			invites = invData.invites;
			players = plData.players;
			whitelistOutput = wlData.output;
			
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
		const edges = [];
		
		nodes.set('ROOT', { id: 'ROOT', label: 'Server Console', shape: 'star', color: '#ffd700' });

		invites.forEach(inv => {
			const creatorId = inv.inviterName || 'ROOT';
			if (!nodes.has(creatorId)) {
				nodes.set(creatorId, { id: creatorId, label: creatorId, shape: 'box', color: '#add8e6' });
			}
			
			if (inv.targetName) {
				nodes.set(inv.targetName, { id: inv.targetName, label: inv.targetName, shape: 'box', color: '#90ee90' });
				edges.push({ from: creatorId, to: inv.targetName, arrows: 'to' });
			} else {
				nodes.set(inv.token, { id: inv.token, label: 'Pending Invite', shape: 'ellipse', color: '#ffcccb' });
				edges.push({ from: creatorId, to: inv.token, arrows: 'to', dashes: true });
			}
		});

		const data = {
			nodes: Array.from(nodes.values()),
			edges: edges
		};

		const options = {
			layout: { hierarchical: { direction: 'UD', sortMethod: 'directed' } },
			physics: false
		};

		if (network) network.destroy();
		network = new Network(networkContainer, data, options);
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

<main class="dashboard">
	<aside class="sidebar">
		<h2>Admin Panel</h2>
		<nav>
			<button class:active={activeTab === 'tree'} on:click={() => switchTab('tree')}>🌳 Invite Tree</button>
			<button class:active={activeTab === 'active'} on:click={() => switchTab('active')}>🎫 Active Invites</button>
			<button class:active={activeTab === 'whitelist'} on:click={() => switchTab('whitelist')}>📋 Players & Whitelist</button>
			<button class:active={activeTab === 'generate'} on:click={() => switchTab('generate')}>🎟️ Generate Invite</button>
			<button class:active={activeTab === 'permanent'} on:click={() => switchTab('permanent')}>🔗 Permanent Links</button>
			<button on:click={() => { localStorage.removeItem('adminPassword'); goto('/admin'); }}>🚪 Logout</button>
		</nav>
	</aside>

	<section class="content">
		{#if loading}
			<p>Loading data...</p>
		{:else if error}
			<p class="error">{error}</p>
		{:else}
			{#if activeTab === 'tree'}
				<div class="card">
					<h3>Invite Tree Visualization</h3>
					<div bind:this={networkContainer} class="network-canvas"></div>
				</div>
			{/if}

			
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

			{#if activeTab === 'whitelist'}
				<div class="card">
					<h3>Currently Online Players (Proxy)</h3>
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
				
				<div class="card">
					<h3>Manage Whitelisted Players ({parsedWhitelistUsers.length} total)</h3>
					<div class="actions-row">
						<form class="inline-form" on:submit|preventDefault={addWhitelist}>
							<input type="text" placeholder="New username..." bind:value={newWhitelistUser} required />
							<button type="submit">Add Player</button>
						</form>
						<input type="text" class="search-bar" placeholder="Search players..." bind:value={whitelistSearchQuery} />
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
	:global(body) { margin: 0; padding: 0; }
	.dashboard {
		display: flex;
		height: 100vh;
		background: #f4f7f6;
		font-family: system-ui, sans-serif;
	}
	.sidebar {
		width: 250px;
		background: #2c3e50;
		color: white;
		padding: 2rem 1rem;
		display: flex;
		flex-direction: column;
		gap: 2rem;
	}
	.sidebar h2 { margin: 0; text-align: center; }
	.sidebar nav {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}
	.sidebar button {
		background: transparent;
		color: white;
		border: none;
		padding: 0.75rem 1rem;
		text-align: left;
		font-size: 1rem;
		cursor: pointer;
		border-radius: 4px;
		transition: background 0.2s;
	}
	.sidebar button:hover { background: #34495e; }
	.sidebar button.active { background: #007cba; }
	
	.content {
		flex: 1;
		padding: 2rem;
		overflow-y: auto;
	}
	.card {
		background: white;
		border-radius: 8px;
		padding: 2rem;
		box-shadow: 0 2px 10px rgba(0,0,0,0.05);
		margin-bottom: 2rem;
	}
	.full-height { height: calc(100vh - 8rem); display: flex; flex-direction: column; }
	.network-canvas { height: 600px; width: 100%; border: 1px solid #ddd; border-radius: 4px; margin-top: 1rem; }
	
	table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
	th, td { text-align: left; padding: 0.75rem; border-bottom: 1px solid #ddd; }
	th { background: #f8f9fa; }
	
	form { display: flex; flex-direction: column; gap: 1rem; max-width: 400px; }
	input { padding: 0.75rem; border: 1px solid #ccc; border-radius: 4px; }
	button[type="submit"] { background: #007cba; color: white; border: none; padding: 0.75rem; border-radius: 4px; cursor: pointer; }
	.warning { color: #856404; background: #fff3cd; padding: 1rem; border-radius: 4px; border: 1px solid #ffeeba; }
	.result-box { margin-top: 1rem; padding: 1rem; background: #d4edda; color: #155724; border-radius: 4px; word-break: break-all; }

	@media (max-width: 768px) {
		.dashboard {
			flex-direction: column;
			height: auto;
			min-height: 100vh;
		}
		.sidebar {
			width: 100%;
			padding: 1rem;
			gap: 1rem;
			box-sizing: border-box;
		}
		.sidebar nav {
			flex-direction: row;
			flex-wrap: wrap;
			gap: 0.5rem;
		}
		.sidebar button {
			flex: 1 1 auto;
			text-align: center;
			font-size: 0.9rem;
			padding: 0.5rem;
		}
		.content {
			padding: 1rem;
		}
		.card {
			padding: 1.5rem;
		}
		table {
			display: block;
			overflow-x: auto;
			white-space: nowrap;
		}
		.full-height {
			height: 50vh;
		}
	}

	.delete-btn { background: #d32f2f !important; color: white; border: none; padding: 0.5rem 1rem; border-radius: 4px; cursor: pointer; transition: background 0.2s; font-size: 0.9rem; }
	.delete-btn:hover { background: #b71c1c !important; }
	.actions-row { display: flex; gap: 1rem; align-items: center; margin-bottom: 1rem; flex-wrap: wrap; justify-content: space-between; }
	.actions-row .inline-form { margin-bottom: 0; }
	.search-bar { padding: 0.75rem; border: 1px solid #ccc; border-radius: 4px; flex: 1; min-width: 200px; max-width: 300px; }
	.admin-badge { background: #ff9800; color: white; padding: 2px 6px; border-radius: 4px; font-size: 0.8rem; font-weight: bold; }
	.inline-form { flex-direction: row; align-items: center; margin-bottom: 1rem; max-width: none; }
	.inline-form input { flex: 1; max-width: 300px; }
	.console-output { background: #1e1e1e; color: #d4d4d4; padding: 1rem; border-radius: 4px; font-family: monospace; max-height: 200px; overflow-y: auto; margin-bottom: 1.5rem; }
	.help-text { font-size: 0.9rem; color: #666; margin-top: -0.5rem; margin-bottom: 1rem; }

</style>
