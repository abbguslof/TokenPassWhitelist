sed -i '546,575d' VelocityPlugin/src/main/java/dev/tokenpass/tokenpasswhitelist/InternalHttpServer.java
cat << 'INNER_EOF' >> VelocityPlugin/src/main/java/dev/tokenpass/tokenpasswhitelist/InternalHttpServer.java
    /**
     * Handles POST /api/add-whitelist
     */
    static class AddWhitelistHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { handlePreflight(exchange); return; }
            if (!checkRateLimit(exchange)) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange, 405, "Method not allowed"); return; }
            String authHeader = exchange.getRequestHeaders().getFirst("X-Admin-Password");
            if (!config.adminPassword.equals(authHeader)) { sendJson(exchange, 401, "Unauthorized"); return; }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String username = json.get("username").getAsString();

            plugin.getServer().getCommandManager().executeAsync(
                    plugin.getServer().getConsoleCommandSource(),
                    config.whitelistCommand + username
            );
            
            sendJson(exchange, 200, "{\"message\":\"Sent add command\"}");
        }
    }
}
INNER_EOF
