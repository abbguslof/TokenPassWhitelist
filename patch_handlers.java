    /**
     * Handles GET /api/whitelist
     */
    static class GetWhitelistHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { handlePreflight(exchange); return; }
            if (!checkRateLimit(exchange)) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendJson(exchange, 405, "Method not allowed"); return; }
            String authHeader = exchange.getRequestHeaders().getFirst("X-Admin-Password");
            if (!config.adminPassword.equals(authHeader)) { sendJson(exchange, 401, "Unauthorized"); return; }

            CapturingCommandSource capturer = new CapturingCommandSource(plugin.getServer().getConsoleCommandSource());
            java.util.concurrent.CompletableFuture<Boolean> future = plugin.getServer().getCommandManager().executeAsync(
                    capturer,
                    config.whitelistListCommand
            );
            
            try {
                // Wait up to 2 seconds for command to execute and print output
                future.get(2, java.util.concurrent.TimeUnit.SECONDS);
                Thread.sleep(200); // Give it a tiny bit more time for messages to arrive
            } catch (Exception e) {
                // Ignore timeouts
            }

            JsonObject res = new JsonObject();
            com.google.gson.JsonArray lines = new com.google.gson.JsonArray();
            for (String line : capturer.getCapturedOutput()) {
                lines.add(line);
            }
            res.add("output", lines);
            sendJson(exchange, 200, res);
        }
    }

    /**
     * Handles POST /api/remove-whitelist
     */
    static class RemoveWhitelistHandler implements HttpHandler {
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
                    config.whitelistRemoveCommand + username
            );
            
            sendJson(exchange, 200, "{\"message\":\"Sent remove command\"}");
        }
    }

    /**
     * Handles POST /api/delete-invite
     */
    static class DeleteInviteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { handlePreflight(exchange); return; }
            if (!checkRateLimit(exchange)) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange, 405, "Method not allowed"); return; }
            String authHeader = exchange.getRequestHeaders().getFirst("X-Admin-Password");
            if (!config.adminPassword.equals(authHeader)) { sendJson(exchange, 401, "Unauthorized"); return; }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String token = json.get("token").getAsString();

            InviteStorage.deleteInvite(token);
            sendJson(exchange, 200, "{\"message\":\"Invite deleted\"}");
        }
    }
