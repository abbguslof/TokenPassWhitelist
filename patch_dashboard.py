import re

with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'r') as f:
    content = f.read()

# 1. State changes: add permanentLinks array and local storage logic for activeTab
state_changes = """
	let activeTab = 'tree';
	
	onMount(() => {
		const saved = localStorage.getItem('activeTab');
		if (saved) activeTab = saved;
		
		const pass = localStorage.getItem('adminPassword');
		if (!pass) {
			goto('/admin');
		} else {
			password = pass;
			fetchData();
		}
	});

	function switchTab(tab: string) {
		activeTab = tab;
		localStorage.setItem('activeTab', tab);
		if (tab === 'tree') renderTree();
	}

	let permanentLinks: any[] = [];
"""
content = re.sub(r"\tlet activeTab = 'tree';\n\t\n\tonMount\(\(\) => \{[\s\S]+?\}\);\n\n\tfunction switchTab\(tab: string\) \{[\s\S]+?\}", state_changes.strip(), content)

# 2. Fetch data updates
fetch_changes = """
			const wlData = await apiCall('whitelist-list');
			const permData = await apiCall('permanent-links');
			invites = invData.invites;
			players = plData.players;
			whitelistOutput = wlData.output;
			permanentLinks = permData.permanentLinks;
"""
content = re.sub(r"\t\t\tconst wlData = await apiCall\('whitelist-list'\);\n\t\t\tinvites = invData\.invites;\n\t\t\tplayers = plData\.players;\n\t\t\twhitelistOutput = wlData\.output;", fetch_changes.strip(), content)

# 3. Warning for creating permanent link
create_perm = """
	async function createPermanentLink() {
		if (!confirm('Are you sure you want to generate a permanent link? Anyone with this link (and password, if set) can bypass the whitelist instantly.')) return;
		try {
"""
content = content.replace('\tasync function createPermanentLink() {\n\t\ttry {', create_perm.strip())

# 4. Rendering permanent links inside Active tab
active_tab_html = """
			{#if activeTab === 'active'}
				<div class="card">
					<h3>Permanent Links</h3>
					<table>
						<thead><tr><th>ID</th><th>Creator</th><th>Password?</th><th>Uses</th><th>Date</th><th>Link</th></tr></thead>
						<tbody>
							{#each permanentLinks as p}
								<tr>
									<td>{p.id}</td>
									<td>{p.creatorName}</td>
									<td>{p.hasPassword ? 'Yes' : 'No'}</td>
									<td>{p.uses}</td>
									<td>{new Date(p.createdAt).toLocaleDateString()}</td>
									<td><a href="{window.location.origin}/public-invite/{p.id}" target="_blank">Copy Link</a></td>
								</tr>
							{/each}
							{#if permanentLinks.length === 0}
								<tr><td colspan="6">No permanent links created.</td></tr>
							{/if}
						</tbody>
					</table>
				</div>

				<div class="card">
					<h3>Active Invites (Unclaimed)</h3>
"""
content = content.replace("\t\t\t{#if activeTab === 'active'}\n\t\t\t\t<div class=\"card\">\n\t\t\t\t\t<h3>Active Invites (Unclaimed)</h3>", active_tab_html.strip())

with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'w') as f:
    f.write(content)
