import re

# 1. Fix fetch headers in invite
for path in ['SvelteFrontend/src/routes/invite/[token]/+page.svelte', 'SvelteFrontend/src/routes/public-invite/[id]/+page.svelte']:
    with open(path, 'r') as f:
        content = f.read()
    
    # Add Accept header to fetch
    content = re.sub(
        r"await fetch\(`(/.+?)`\);",
        r"await fetch(`\1`, { headers: { 'Accept': 'application/json' } });",
        content
    )
    with open(path, 'w') as f:
        f.write(content)

# 2. Add delete permanent link to dashboard backend proxy
with open('SvelteFrontend/src/routes/admin/dashboard/api/+server.ts', 'r') as f:
    content = f.read()

# No need to add delete-permanent to GET list, it is a POST request
with open('SvelteFrontend/src/routes/admin/dashboard/api/+server.ts', 'w') as f:
    f.write(content)

# 3. Add delete permanent link to dashboard frontend
with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'r') as f:
    content = f.read()

delete_func = """
	async function deletePermanentLink(id: string) {
		if (confirm('Are you sure you want to delete this permanent link?')) {
			try {
				await apiCall('delete-permanent-link', { id });
				await fetchData();
			} catch (e: any) { alert(e.message); }
		}
	}
"""
content = content.replace('async function deleteInvite(token: string) {', delete_func.strip() + '\n\n\tasync function deleteInvite(token: string) {')

# Add button to table
old_table_row = """									<td>{new Date(p.createdAt).toLocaleDateString()}</td>
									<td><a href="{window.location.origin}/public-invite/{p.id}" target="_blank">Copy Link</a></td>"""
new_table_row = """									<td>{new Date(p.createdAt).toLocaleDateString()}</td>
									<td>
										<a href="{window.location.origin}/public-invite/{p.id}" target="_blank">Copy Link</a>
										<button class="delete-btn" style="margin-left: 0.5rem; padding: 0.25rem 0.5rem; font-size: 0.8rem;" on:click={() => deletePermanentLink(p.id)}>Delete</button>
									</td>"""
content = content.replace(old_table_row, new_table_row)

with open('SvelteFrontend/src/routes/admin/dashboard/+page.svelte', 'w') as f:
    f.write(content)

# 4. Add backend Java handler for delete-permanent-link
with open('VelocityPlugin/src/main/java/dev/tokenpass/tokenpasswhitelist/InternalHttpServer.java', 'r') as f:
    content = f.read()

if "DeletePermanentLinkHandler" not in content:
    register_target = 'server.createContext("/api/delete-invite", new DeleteInviteHandler());'
    register_replacement = 'server.createContext("/api/delete-permanent-link", new DeletePermanentLinkHandler());\n            ' + register_target
    content = content.replace(register_target, register_replacement)

    handler_code = """
    /**
     * Handles POST /api/delete-permanent-link
     * Deletes a permanent link.
     * Requires the X-Admin-Password header.
     */
    static class DeletePermanentLinkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { handlePreflight(exchange); return; }
            if (!checkRateLimit(exchange)) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange, 405, "Method not allowed"); return; }
            String authHeader = exchange.getRequestHeaders().getFirst("X-Admin-Password");
            if (!config.adminPassword.equals(authHeader)) { sendJson(exchange, 401, "Unauthorized"); return; }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String id = json.get("id").getAsString();

            InviteStorage.deletePermanentLink(id);
            sendJson(exchange, 200, "Permanent link deleted");
        }
    }

    /**
     * Handles POST /api/delete-invite
"""
    content = content.replace('    /**\n     * Handles POST /api/delete-invite', handler_code)

with open('VelocityPlugin/src/main/java/dev/tokenpass/tokenpasswhitelist/InternalHttpServer.java', 'w') as f:
    f.write(content)

# 5. Add deletePermanentLink to InviteStorage
with open('VelocityPlugin/src/main/java/dev/tokenpass/tokenpasswhitelist/InviteStorage.java', 'r') as f:
    content = f.read()

if "deletePermanentLink" not in content:
    delete_code = """
    public static void deletePermanentLink(String id) {
        permanentLinks.remove(id);
        savePermanentLinksToDisk();
    }
"""
    content = content.replace('public static void deleteInvite(String token) {', delete_code.strip() + '\n\n    public static void deleteInvite(String token) {')

with open('VelocityPlugin/src/main/java/dev/tokenpass/tokenpasswhitelist/InviteStorage.java', 'w') as f:
    f.write(content)

