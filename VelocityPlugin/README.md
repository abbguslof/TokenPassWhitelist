# TokenPassWhitelist (Velocity Plugin)

**TokenPassWhitelist** is a Velocity proxy plugin designed to provide a secure, invite-based whitelisting system for Minecraft servers. Players can invite others using `/invite`, generating unique, one-time-use invite links that can be claimed via a simple frontend website. The plugin supports external HTTP interaction, web-based invites, admin tools, persistence, and rate-limiting.

> ✅ Part of the larger **TokenPassWhitelist** system, which includes a frontend built in Svelte for link management and claim flow.

---

## 🧩 Features

- `/invite` command for players to generate whitelist links
- `/invite list` command to view your generated invites and their status
- Admin web interface to create invites externally
- One-time token usage for secure access control
- Internal HTTP API server with rate limiting
- Whitelisting via Velocity-compatible command
- Cross-container/frontend compatibility
- Invite persistence using YAML file storage
- Feedback to inviter when their invite is redeemed
- CORS-ready API (suitable for frontend deployments)
- Clickable invite links with hover tooltips in chat

---

## 📁 Folder Structure

This folder contains the Velocity plugin only. For the full project, see the root of this repository.

```bash
VelocityPlugin/
├── src/
│   └── main/
│       ├── java/dev/tokenpass/tokenpasswhitelist/
│       │   ├── TokenPassWhitelist.java         # Main plugin entry
│       │   ├── InternalHttpServer.java         # Built-in API server
│       │   ├── InviteStorage.java              # Invite logic and persistence
│       │   ├── InviteCommand.java              # /invite command handler
│       │   └── ConfigFile.java                 # YAML config loader
│       └── resources/
│           └── config.yml                      # Default configuration
└── pom.xml                                     # Maven build file
```

---

## ⚙️ Building the Plugin

This project uses **Maven** to build the plugin. Make sure you have Maven installed.

```bash
mvn clean package
```

After building, the compiled plugin JAR will be available at:

```
target/TokenPassWhitelist-VERSION.jar
```

Place this JAR inside your Velocity server's `plugins/` folder.

---

## 🧪 Running the Plugin

1. Ensure Velocity is running
2. After the plugin is placed in `plugins/`, start the server once to generate the config
3. Edit the `config.yml` file with your desired settings (see below)
4. Restart the server

---

## 🔧 Configuration (config.yml)

```yaml
ip: 0.0.0.0                  # Bind IP for internal HTTP server
port: 5000                   # Port for internal HTTP server
api_secret: REPLACE_ME       # Secret for API requests from frontend
admin_password: change_me_now # Admin panel password
website_domain: example.com  # Used to construct invite links
whitelist_command: whitelist add   # Command used to whitelist players
```

**Important:** Make sure your port is accessible from wherever the frontend is hosted.

---

## 📌 Commands

### `/invite`
Generates a new invite link and prints it to the user.
- **Usage:** `/invite`
- **Permission:** Must be a player (console usage not supported for invite generation)
- **Output:** Clickable invite link with hover tooltip

### `/invite list`
Shows all invites you've created and their status.
- **Usage:** `/invite list`
- **Permission:** Must be a player
- **Output:** List of your invites, showing which have been used and by whom

---

## 🌐 Internal HTTP API

The plugin exposes a small HTTP server to interact with the invite system from the frontend.

### Endpoints

| Method | Endpoint | Description | Headers |
|--------|----------|-------------|---------|
| POST | `/api/whitelist` | Redeems an invite, whitelists player | `X-Auth-Token` |
| POST | `/api/check-token` | Checks if token is valid | `X-Auth-Token` |
| POST | `/api/invite-admin` | Admin-only invite creation | `X-Admin-Password` |
| GET | `/ping` | Health check | None |

All endpoints support CORS and preflight OPTIONS requests.

### API Examples

**Redeem an invite:**
```bash
curl -X POST http://localhost:5000/api/whitelist \
  -H "X-Auth-Token: YOUR_API_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"token": "invite-token", "username": "PlayerName"}'
```

**Check token validity:**
```bash
curl -X POST http://localhost:5000/api/check-token \
  -H "X-Auth-Token: YOUR_API_SECRET" \
  -H "Content-Type: application/json" \
  -d '{"token": "invite-token"}'
```

---

## 📂 File Overview

### `TokenPassWhitelist.java`
- **Purpose:** Entry point for the plugin
- **Responsibilities:**
  - Registers the `/invite` command
  - Initializes the HTTP server
  - Loads configuration
  - Sets up invite storage persistence

### `InternalHttpServer.java`
- **Purpose:** Embedded HTTP server using Java's built-in `HttpServer`
- **Responsibilities:**
  - Handles API endpoints for frontend integration
  - Implements rate limiting (5 requests per 10 seconds per IP)
  - Manages CORS headers for cross-origin requests
  - Processes invite redemption and validation

### `InviteStorage.java`
- **Purpose:** Core invite management system
- **Responsibilities:**
  - Token creation, validation, and claiming
  - YAML file-based persistence (`invites.yml`)
  - Tracks inviter-invitee relationships
  - Handles both player-generated and admin-generated invites

### `InviteCommand.java`
- **Purpose:** Handles the `/invite` command
- **Responsibilities:**
  - Generates new invite tokens for players
  - Creates clickable invite links with Adventure components
  - Lists player's existing invites and their status

### `ConfigFile.java`
- **Purpose:** Configuration management
- **Responsibilities:**
  - Loads settings from `config.yml`
  - Auto-generates default config on first run
  - Uses Sponge Configurate for YAML parsing

---

## 🔐 Rate Limiting

Each API endpoint is protected by IP-based rate limiting:

- **Limit:** Max 5 requests per 10 seconds per IP
- **Purpose:** Prevents abuse and automated spam
- **Response:** 429 Too Many Requests when exceeded

---

## 📦 Persistence

All invites (active and claimed) are saved to `invites.yml` in your plugin data folder.

**Features:**
- Survives server restarts
- Tracks invite creation timestamps
- Stores inviter UUID and name
- Records when invites are claimed and by whom
- Supports both player-generated and web-generated invites

**Example `invites.yml` structure:**
```yaml
"uuid-token-here":
  inviterUUID: "player-uuid-here"
  inviterName: "PlayerName"
  targetName: "InvitedPlayer"
  createdAt: 1640995200000
```

---

## 💡 Frontend Integration

The plugin is designed to work seamlessly with the included Svelte frontend in the `../frontend/` folder. The frontend:

- Accepts invite links (`https://yourdomain.com/invite/token`)
- Validates tokens with the API
- Whitelists players on form submission
- Provides admin UI to generate invites
- Handles error states and user feedback

Read the `frontend/README.md` for setup instructions.

---

## 🌍 Deployment Tips

1. **Reverse Proxy:** Use Nginx or Nginx Proxy Manager to proxy `/api/*` from your public domain to the plugin server port
2. **Security:** Protect `/api/invite-admin` with a strong password
3. **Environment:** Set your `.env` in the frontend with `VITE_API_URL` pointing to the proxy (e.g., `https://yourdomain.com/api`)
4. **Firewall:** Ensure the configured port is accessible from your frontend deployment

**Example Nginx configuration:**
```nginx
location /api/ {
    proxy_pass http://minecraft-server:5000/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
```

---

## 🧪 Development Notes

**Dependencies:**
- Java 17+
- Velocity API
- Built-in HTTP server (no Netty or third-party web frameworks)
- Gson for JSON parsing
- Sponge Configurate for YAML support
- Adventure API for rich text components

**Architecture:**
- Thread-safe invite storage with `ConcurrentHashMap`
- Async command execution for whitelist operations
- Event-driven player notifications
- Immutable configuration objects

---

## 🔧 Troubleshooting

**Plugin won't start:**
- Check Java version (requires 17+)
- Verify Velocity compatibility
- Check server logs for specific errors

**HTTP server not accessible:**
- Verify port configuration in `config.yml`
- Check firewall settings
- Ensure IP binding is correct (`0.0.0.0` for all interfaces)

**Invites not persisting:**
- Check file permissions in plugin data directory
- Verify `invites.yml` is being created
- Look for I/O errors in server logs

---

## 📥 Contributing

Pull requests and suggestions are welcome! Please:

- Follow Java conventions
- Document public methods with JavaDoc
- Test functionality before submitting
- Maintain thread safety in concurrent operations

---

## 📄 License

This project is licensed under the MIT License. See LICENSE for details.

---

## ✨ Credits

Built with ❤️ by @guslof and the community.