package dev.tokenpass.tokenpasswhitelist;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InternalHttpServer {

    private static HttpServer server;
    private static TokenPassWhitelist plugin;
    private static ConfigFile config;
    private static final Gson gson = new Gson();

    private static final Map<String, RequestBucket> rateLimits = new ConcurrentHashMap<>();
    private static final int RATE_LIMIT_MAX = 100;
    private static final int RATE_LIMIT_SECONDS = 10;

    public static void start(TokenPassWhitelist pluginInstance, ConfigFile loadedConfig) {
        plugin = pluginInstance;
        config = loadedConfig;

        plugin.getLogger().info("[TokenPassWhitelist] Starting internal HTTP server...");

        try {
            server = HttpServer.create(new InetSocketAddress(config.ip, config.port), 0);
            server.createContext("/api/whitelist", new WhitelistHandler());
            server.createContext("/api/invite-admin", new AdminInviteHandler());
            server.createContext("/api/check-token", new CheckTokenHandler());
            server.createContext("/api/invites", new GetInvitesHandler());
            server.createContext("/api/players", new GetPlayersHandler());
            server.createContext("/api/permanent-links", new GetPermanentLinksHandler());
            server.createContext("/api/permanent-link", new CreatePermanentLinkHandler());
            server.createContext("/api/permanent-link-info", new GetPermanentLinkInfoHandler());
            server.createContext("/api/permanent-whitelist", new WhitelistPermanentHandler());
            server.createContext("/api/ping", new PingHandler());
            server.createContext("/api/whitelist-list", new GetWhitelistHandler());
            server.createContext("/api/add-whitelist", new AddWhitelistHandler());
            server.createContext("/api/remove-whitelist", new RemoveWhitelistHandler());
            server.createContext("/api/delete-permanent-link", new DeletePermanentLinkHandler());
            server.createContext("/api/delete-invite", new DeleteInviteHandler());
            server.setExecutor(null); // default executor

            server.start();
            plugin.getLogger().info("[TokenPassWhitelist] HTTP server started on " + config.ip + ":" + config.port);
        } catch (IOException e) {
            plugin.getLogger().error("[TokenPassWhitelist] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles POST /api/whitelist
     * Redeems a one-time use token and whitelists the given user.
     * Requires the X-Auth-Token header matching the configured api_secret.
     */
    static class WhitelistHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            plugin.getLogger().info("[TokenPassWhitelist] Received request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handlePreflight(exchange);
                return;
            }

            if (!checkRateLimit(exchange)) return;

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, "Method not allowed");
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("X-Auth-Token");
            plugin.getLogger().info("[TokenPassWhitelist] Auth header: " + authHeader);

            if (!config.apiSecret.equals(authHeader)) {
                sendJson(exchange, 401, "Unauthorized");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);

            String token = json.get("token").getAsString();
            String username = json.get("username").getAsString().trim();

            if (!username.matches("^[a-zA-Z0-9_.*-]{3,24}$")) {
                sendJson(exchange, 400, "Invalid username format");
                return;
            }

            InviteStorage.InviteEntry entry = InviteStorage.useToken(token, username);
            if (entry == null) {
                sendJson(exchange, 400, "Invalid or expired token");
                return;
            }

            // Whitelist command
            plugin.getServer().getCommandManager().executeAsync(
                    plugin.getServer().getConsoleCommandSource(),
                    config.whitelistCommand + username
            );

            // Notify inviter if online
            if (entry.inviterUUID != null) {
                plugin.getServer().getPlayer(entry.inviterUUID).ifPresent(inviter -> {
                    inviter.sendMessage(Component.text(
                            "Your invite was used to whitelist " + username + "!",
                            NamedTextColor.GREEN
                    ));
                });
            }

            sendJson(exchange, 200, "User whitelisted successfully");
        }
    }

    /**
     * Handles POST /api/invite-admin
     * Creates a new one-time use invite token from the admin panel.
     * Requires the X-Admin-Password header matching the configured admin_password.
     */
    static class AdminInviteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handlePreflight(exchange);
                return;
            }

            if (!checkRateLimit(exchange)) return;

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, "Method not allowed");
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("X-Admin-Password");
            if (!config.adminPassword.equals(authHeader)) {
                sendJson(exchange, 401, "Unauthorized");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);

            String baseInviter = json.has("inviterName") && !json.get("inviterName").isJsonNull() && !json.get("inviterName").getAsString().isEmpty() ? json.get("inviterName").getAsString() : "Console";
            String inviterName = "[Admin] " + baseInviter;

            String token = UUID.randomUUID().toString();
            InviteStorage.inviteFromWeb(token, inviterName, null);

            JsonObject response = new JsonObject();
            response.addProperty("token", token);
            response.addProperty("link", "https://" + config.websiteDomain + "/invite/" + token);

            sendJson(exchange, 200, response);
        }
    }

    /**
     * Handles POST /api/check-token
     * Validates whether a given one-time token exists and is unused.
     * Requires the X-Auth-Token header.
     */
    static class CheckTokenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handlePreflight(exchange);
                return;
            }

            if (!checkRateLimit(exchange)) return;

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendJson(exchange, 405, "Method not allowed");
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("X-Auth-Token");
            if (!config.apiSecret.equals(authHeader)) {
                sendJson(exchange, 401, "Unauthorized");
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String token = json.get("token").getAsString();

            if (InviteStorage.isValidToken(token)) {
                sendJson(exchange, 200, "Token is valid");
            } else {
                sendJson(exchange, 400, "Invalid token");
            }
        }
    }

    /**
     * Handles GET /api/invites
     * Retrieves all invites (active and claimed) for the dashboard tree.
     * Requires the X-Admin-Password header.
     */
    static class GetInvitesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { handlePreflight(exchange); return; }
            if (!checkRateLimit(exchange)) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendJson(exchange, 405, "Method not allowed"); return; }
            String authHeader = exchange.getRequestHeaders().getFirst("X-Admin-Password");
            if (!config.adminPassword.equals(authHeader)) { sendJson(exchange, 401, "Unauthorized"); return; }

            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (Map.Entry<String, InviteStorage.InviteEntry> e : InviteStorage.getAllInvites().entrySet()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("token", e.getKey());
                obj.addProperty("inviterUUID", e.getValue().inviterUUID == null ? null : e.getValue().inviterUUID.toString());
                obj.addProperty("inviterName", e.getValue().inviterName);
                obj.addProperty("targetName", e.getValue().targetName);
                obj.addProperty("createdAt", e.getValue().createdAt);
                arr.add(obj);
            }
            JsonObject res = new JsonObject();
            res.add("invites", arr);
            sendJson(exchange, 200, res);
        }
    }

    /**
     * Handles GET /api/players
     * Retrieves a list of all currently online players across the proxy.
     * Requires the X-Admin-Password header.
     */
    static class GetPlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { handlePreflight(exchange); return; }
            if (!checkRateLimit(exchange)) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { sendJson(exchange, 405, "Method not allowed"); return; }
            String authHeader = exchange.getRequestHeaders().getFirst("X-Admin-Password");
            if (!config.adminPassword.equals(authHeader)) { sendJson(exchange, 401, "Unauthorized"); return; }

            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (com.velocitypowered.api.proxy.Player p : plugin.getServer().getAllPlayers()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("uuid", p.getUniqueId().toString());
                obj.addProperty("username", p.getUsername());
                arr.add(obj);
            }
            JsonObject res = new JsonObject();
            res.add("players", arr);
            sendJson(exchange, 200, res);
        }
    }

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
     * Creates a new permanent, reusable invite link.
     * Requires the X-Admin-Password header.
     */
    static class CreatePermanentLinkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { handlePreflight(exchange); return; }
            if (!checkRateLimit(exchange)) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange, 405, "Method not allowed"); return; }
            String authHeader = exchange.getRequestHeaders().getFirst("X-Admin-Password");
            if (!config.adminPassword.equals(authHeader)) { sendJson(exchange, 401, "Unauthorized"); return; }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String creatorName = json.has("creatorName") ? json.get("creatorName").getAsString() : "[admin]";
            String passwordHash = json.has("passwordHash") && !json.get("passwordHash").isJsonNull() ? json.get("passwordHash").getAsString() : null;

            String id = UUID.randomUUID().toString();
            InviteStorage.createPermanentLink(id, creatorName, passwordHash);

            JsonObject res = new JsonObject();
            res.addProperty("id", id);
            sendJson(exchange, 200, res);
        }
    }

    /**
     * Handles POST /api/permanent-link-info
     * Returns public information about a permanent link (e.g. if it requires a password).
     * Requires the X-Auth-Token header.
     */
    static class GetPermanentLinkInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { handlePreflight(exchange); return; }
            if (!checkRateLimit(exchange)) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange, 405, "Method not allowed"); return; }
            String authHeader = exchange.getRequestHeaders().getFirst("X-Auth-Token");
            if (!config.apiSecret.equals(authHeader)) { sendJson(exchange, 401, "Unauthorized"); return; }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String id = json.get("id").getAsString();

            InviteStorage.PermanentLink link = InviteStorage.getPermanentLink(id);
            if (link == null) {
                sendJson(exchange, 404, "Not found");
                return;
            }

            JsonObject res = new JsonObject();
            res.addProperty("hasPassword", link.passwordHash != null);
            res.addProperty("creatorName", link.creatorName);
            sendJson(exchange, 200, res);
        }
    }

    /**
     * Handles POST /api/permanent-whitelist
     * Claims a permanent link, validating the password if required, and whitelists the user.
     * Requires the X-Auth-Token header.
     */
    static class WhitelistPermanentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) { handlePreflight(exchange); return; }
            if (!checkRateLimit(exchange)) return;
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) { sendJson(exchange, 405, "Method not allowed"); return; }
            String authHeader = exchange.getRequestHeaders().getFirst("X-Auth-Token");
            if (!config.apiSecret.equals(authHeader)) { sendJson(exchange, 401, "Unauthorized"); return; }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String id = json.get("id").getAsString();
            String username = json.get("username").getAsString().trim();
            String passwordHash = json.has("passwordHash") && !json.get("passwordHash").isJsonNull() ? json.get("passwordHash").getAsString() : null;

            if (!username.matches("^[a-zA-Z0-9_.*-]{3,24}$")) {
                sendJson(exchange, 400, "Invalid username format");
                return;
            }

            InviteStorage.PermanentLink link = InviteStorage.getPermanentLink(id);
            if (link == null) {
                sendJson(exchange, 404, "Invalid link");
                return;
            }

            if (link.passwordHash != null && !link.passwordHash.equals(passwordHash)) {
                sendJson(exchange, 401, "Invalid password");
                return;
            }

            String dummyToken = "perm_" + UUID.randomUUID().toString();
            InviteStorage.inviteFromWeb(dummyToken, link.creatorName, username);

            plugin.getServer().getCommandManager().executeAsync(
                    plugin.getServer().getConsoleCommandSource(),
                    config.whitelistCommand + username
            );

            sendJson(exchange, 200, "User whitelisted successfully");
        }
    }

    /**
     * Handles GET /ping
     * Simple health check endpoint used to verify the server is running.
     * No authentication required.
     */
    static class PingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                handlePreflight(exchange);
                return;
            }

            sendJson(exchange, 200, "pong");
        }
    }

    // --- Rate limiting logic ---
    private static boolean checkRateLimit(HttpExchange exchange) throws IOException {
        if (Math.random() < 0.05) {
            long now = Instant.now().getEpochSecond();
            rateLimits.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        }

        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        RequestBucket bucket = rateLimits.computeIfAbsent(ip, k -> new RequestBucket());

        if (!bucket.allow()) {
            sendJson(exchange, 429, "Too many requests. Please wait and try again.");
            return false;
        }

        return true;
    }

    private static class RequestBucket {
        int count;
        long expiresAt;

        RequestBucket() {
            this.count = 1;
            this.expiresAt = Instant.now().getEpochSecond() + RATE_LIMIT_SECONDS;
        }

        boolean allow() {
            long now = Instant.now().getEpochSecond();
            if (now > expiresAt) {
                count = 1;
                expiresAt = now + RATE_LIMIT_SECONDS;
                return true;
            }
            if (count < RATE_LIMIT_MAX) {
                count++;
                return true;
            }
            return false;
        }
    }

    private static void setCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null && (origin.endsWith(config.websiteDomain) || origin.startsWith("http://localhost"))) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
        } else {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "https://" + config.websiteDomain);
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Auth-Token, X-Admin-Password");
    }

    private static void handlePreflight(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1); // No content
        exchange.close();
    }

    private static void sendJson(HttpExchange exchange, int code, String message) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("message", message);
        sendJson(exchange, code, json);
    }

    private static void sendJson(HttpExchange exchange, int code, JsonObject json) throws IOException {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        setCorsHeaders(exchange);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

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
            String username = json.get("username").getAsString().trim();

            plugin.getServer().getCommandManager().executeAsync(
                    plugin.getServer().getConsoleCommandSource(),
                    config.whitelistRemoveCommand + username
            );
            
            sendJson(exchange, 200, "{\"message\":\"Sent remove command\"}");
        }
    }


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
            String username = json.get("username").getAsString().trim();

            plugin.getServer().getCommandManager().executeAsync(
                    plugin.getServer().getConsoleCommandSource(),
                    config.whitelistCommand + username
            );
            
            sendJson(exchange, 200, "{\"message\":\"Sent add command\"}");
        }
    }
}
