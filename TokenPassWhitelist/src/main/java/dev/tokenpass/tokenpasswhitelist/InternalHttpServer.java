package dev.tokenpass.tokenpasswhitelist;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class InternalHttpServer {

    private static HttpServer server;
    private static TokenPassWhitelist plugin;
    private static ConfigFile config;
    private static final Gson gson = new Gson();

    public static void start(TokenPassWhitelist pluginInstance, ConfigFile loadedConfig) {
        plugin = pluginInstance;
        config = loadedConfig;

        plugin.getLogger().info("[TokenPassWhitelist] Attempting to start internal HTTP server...");

        try {
            server = HttpServer.create(new InetSocketAddress(config.ip, config.port), 0);

            plugin.getLogger().info("[TokenPassWhitelist] Binding to " + config.ip + ":" + config.port);

            server.createContext("/api/whitelist", new WhitelistHandler());
            server.createContext("/api/admin-invite", new AdminInviteHandler());
            server.createContext("/ping", new PingHandler());

            plugin.getLogger().info("[TokenPassWhitelist] Contexts registered. Starting server...");

            server.setExecutor(null); // Use default executor
            server.start();

            plugin.getLogger().info("[TokenPassWhitelist] Internal HTTP server started successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("[TokenPassWhitelist] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static class PingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "server is responding to ping request.";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    static class WhitelistHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            plugin.getLogger().info("[TokenPassWhitelist] Received request: " + exchange.getRequestMethod());

            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                plugin.getLogger().info("[TokenPassWhitelist] Rejected: Not a POST");
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("X-Auth-Token");
            plugin.getLogger().info("[TokenPassWhitelist] Auth header: " + authHeader);

            if (authHeader == null || !authHeader.equals(config.apiSecret)) {
                plugin.getLogger().info("[TokenPassWhitelist] Rejected: Invalid or missing auth token");
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            JsonObject body;
            try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                body = gson.fromJson(reader, JsonObject.class);
            }

            String token = body.get("token").getAsString();
            String username = body.get("username").getAsString();

            plugin.getLogger().info("[TokenPassWhitelist] Request to whitelist " + username + " using token " + token);

            if (!InviteStorage.isValidToken(token)) {
                sendJson(exchange, 400, "Invalid or expired token.");
                return;
            }

            InviteStorage.InviteEntry entry = InviteStorage.useToken(token);
            if (entry == null) {
                sendJson(exchange, 400, "Token already used.");
                return;
            }

            // Run the whitelist add command
            String command = config.whitelistCommand + username;
            plugin.getLogger().info("[TokenPassWhitelist] Running command: " + command);
            plugin.getServer().getCommandManager().executeAsync(plugin.getServer().getConsoleCommandSource(), command);
            plugin.getLogger().info("[TokenPassWhitelist] " + username + " whitelisted (invited by " + entry.inviterName + ")");

            sendJson(exchange, 200, "Player " + username + " has been whitelisted.");
        }

        private void sendJson(HttpExchange exchange, int code, String message) throws IOException {
            JsonObject response = new JsonObject();
            response.addProperty("message", message);
            byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class AdminInviteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String adminPassword = exchange.getRequestHeaders().getFirst("X-Admin-Password");
            if (adminPassword == null || !adminPassword.equals(config.adminPassword)) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            JsonObject body;
            try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                body = gson.fromJson(reader, JsonObject.class);
            }

            String username = body.get("username").getAsString();
            String inviterName = body.has("inviterName") ? body.get("inviterName").getAsString() : "WebAdmin";

            String token = UUID.randomUUID().toString();
            InviteStorage.inviteFromWeb(token, inviterName, username);

            String link = "https://" + config.websiteDomain + "/invite/" + token;
            JsonObject response = new JsonObject();
            response.addProperty("message", "Invite created: " + link);

            byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
