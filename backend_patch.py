import re

with open('VelocityPlugin/src/main/java/dev/tokenpass/tokenpasswhitelist/InternalHttpServer.java', 'r') as f:
    content = f.read()

# 1. Register the endpoint
register_target = 'server.createContext("/api/permanent-link", new CreatePermanentLinkHandler());'
register_replacement = 'server.createContext("/api/permanent-links", new GetPermanentLinksHandler());\n            ' + register_target
content = content.replace(register_target, register_replacement)

# 2. Add GetPermanentLinksHandler
handler_code = """
    /**
     * Handles GET /api/permanent-links
     * Retrieves all permanent links and their usage count.
     * Requires the X-Admin-Password header.
     */
    static class GetPermanentLinksHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { handlePreflight(exchange); return; }
            if (!checkRateLimit(exchange)) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendJson(exchange, 405, "Method not allowed"); return; }
            String authHeader = exchange.getRequestHeaders().getFirst("X-Admin-Password");
            if (!config.adminPassword.equals(authHeader)) { sendJson(exchange, 401, "Unauthorized"); return; }

            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            Map<String, InviteStorage.InviteEntry> allInvites = InviteStorage.getAllInvites();

            for (Map.Entry<String, InviteStorage.PermanentLink> e : InviteStorage.getAllPermanentLinks().entrySet()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", e.getKey());
                obj.addProperty("creatorName", e.getValue().creatorName);
                obj.addProperty("hasPassword", e.getValue().passwordHash != null);
                obj.addProperty("createdAt", e.getValue().createdAt);
                
                long uses = allInvites.values().stream()
                    .filter(inv -> e.getValue().creatorName.equals(inv.inviterName))
                    .count();
                obj.addProperty("uses", uses);
                
                arr.add(obj);
            }
            JsonObject res = new JsonObject();
            res.add("permanentLinks", arr);
            sendJson(exchange, 200, res);
        }
    }

    /**
     * Handles POST /api/permanent-link
"""
content = content.replace('    /**\n     * Handles POST /api/permanent-link', handler_code)

with open('VelocityPlugin/src/main/java/dev/tokenpass/tokenpasswhitelist/InternalHttpServer.java', 'w') as f:
    f.write(content)
