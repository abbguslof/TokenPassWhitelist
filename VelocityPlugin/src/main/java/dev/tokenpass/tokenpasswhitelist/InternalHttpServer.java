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
    private static final int RATE_LIMIT_MAX = 5;
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
            server.createContext("/ping", new PingHandler());
            server.setExecutor(null); // default executor

            server.start();
            plugin.getLogger().info("[TokenPassWhitelist] HTTP server started on " + config.ip + ":" + config.port);
        } catch (IOException e) {
            plugin.getLogger().severe("[TokenPassWhitelist] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

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
            String username = json.get("username").getAsString();

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

            String username = json.get("username").getAsString();
            String inviterName = json.get("inviterName").getAsString();

            String token = UUID.randomUUID().toString();
            InviteStorage.inviteFromWeb(token, inviterName, username);

            JsonObject response = new JsonObject();
            response.addProperty("token", token);
            response.addProperty("link", "https://" + config.websiteDomain + "/invite/" + token);

            sendJson(exchange, 200, response);
        }
    }

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

    private static void handlePreflight(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, X-Auth-Token, X-Admin-Password");
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
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Auth-Token, X-Admin-Password");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
