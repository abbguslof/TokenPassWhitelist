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

        plugin.getLogger().info("[TokenPassWhitelist] Starting internal HTTP server...");

        try {
            server = HttpServer.create(new InetSocketAddress(config.ip, config.port), 0);
            server.createContext("/api/whitelist", new WhitelistHandler());
            server.createContext("/api/invite-admin", new AdminInviteHandler());
            server.createContext("/ping", new PingHandler());
            server.setExecutor(null); // default executor

            server.start();
            plugin.getLogger().info("[TokenPassWhitelist] HTTP server started on " + config.ip + ":" + config.port);
        } catch (IOException e) {
            plugin.getLogger().severe("[TokenPassWhitelist] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // WhitelistHandler (whitelisted from website using token)
    static class WhitelistHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("X-Auth-Token");
            if (authHeader == null || !authHeader.equals(config.apiSecret)) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            JsonObject body;
            try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
                body = gson.fromJson(reader, JsonObject.class);
            }

            String token = body.get("token").getAsString();
            String username = body.get("username").getAsString();

            if (!InviteStorage.isValidToken(token)) {
                sendJson(exchange, 400, "Invalid or expired token.");
                return;
            }

            InviteStorage.InviteEntry used = InviteStorage.useToken(token, username);
            if (used == null) {
                sendJson(exchange, 400, "Token already used or invalid.");
                return;
            }

            String command = config.whitelistCommand + username;
            plugin.getServer().getCommandManager().executeAsync(plugin.getServer().getConsoleCommandSource(), command);

            plugin.getLogger().info("[TokenPassWhitelist] " + username + " whitelisted (invited by " + used.inviterName + ")");
            sendJson(exchange, 200, "Player " + username + " has been whitelisted.");
        }
    }

    // AdminInviteHandler (admin panel creates invite)
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
            response.addProperty("message", "Invite created.");
            response.addProperty("token", token);
            response.addProperty("link", link);

            sendJson(exchange, 200, response);
        }
    }

    // PingHandler and sendJson
    static class PingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "pong";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private static void sendJson(HttpExchange exchange, int code, String message) throws IOException {
        JsonObject obj = new JsonObject();
        obj.addProperty("message", message);
        sendJson(exchange, code, obj);
    }

    private static void sendJson(HttpExchange exchange, int code, JsonObject json) throws IOException {
        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}