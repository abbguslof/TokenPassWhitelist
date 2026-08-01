# TokenPassWhitelist - Secure Minecraft Whitelist System

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Velocity](https://img.shields.io/badge/Velocity-3.0+-blue.svg)](https://velocitypowered.com/)
[![SvelteKit](https://img.shields.io/badge/SvelteKit-2.x-red.svg)](https://kit.svelte.dev/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A modern, secure invite-based whitelisting system for Minecraft servers running on Velocity proxy. TokenPassWhitelist combines a robust server-side plugin with a sleek web frontend to provide a seamless invitation and registration experience.

## 🎯 What This Project Does

TokenPassWhitelist transforms the traditional Minecraft whitelist process into a secure, user-friendly system:

1. **Server administrators or trusted players** generate unique invite links using in-game commands or a feature-rich web admin panel.
2. **Invited users** click the link, complete CAPTCHA verification, and confirm their Minecraft username.
3. **The system automatically** adds them to the server whitelist and notifies the inviter.
4. **Administrators** can manage everything from a web dashboard — generate invites, create permanent links, manage the whitelist, and visualize who invited whom.

## 🏗️ System Architecture

```
┌─────────────────┐    HTTP API     ┌──────────────────┐
│   Web Frontend  │ ◄──────────────► │  Velocity Plugin │
│   (SvelteKit)   │                  │    (Java 17+)    │
└─────────────────┘                  └──────────────────┘
         │                                     │
         │                                     │
    ┌────▼────┐                           ┌────▼────┐
    │ hCaptcha│                           │Minecraft│
    │   CDN   │                           │ Server  │
    └─────────┘                           └─────────┘
```

### Core Components

- **[Velocity Plugin](VelocityPlugin/)** - Server-side Java plugin with a built-in HTTP API server.
- **[Web Frontend](SvelteFrontend/)** - Modern SvelteKit application for invite management, permanent links, and whitelist administration.
- **Security Layer** - hCaptcha verification, IP-based rate limiting, and token validation on all endpoints.

## ✨ Key Features

### 🎮 Player Experience
- **In-game invite generation** with `/invite` command.
- **Clickable invite links** in chat with hover tooltips.
- **Real-time notifications** when invites are redeemed.
- **Invite tracking** with `/invite list` command.
- **Bedrock support** — username validation accepts Geyser-prefixed names (e.g., `.player`, `*player`).
- **Mobile-friendly** web interface.

### 👨‍💼 Comprehensive Admin Dashboard
- **Interactive Invite Tree**: Visualize who invited whom with an interactive, draggable node graph built with `vis-network`. Nodes are color-coded: green for whitelisted players, red for removed players, pink for pending invites, and blue for inviters. Click any node to select it and remove its record from the tree.
- **Active Invites & Permanent Links**: View all unclaimed one-time invites and all active permanent links in a single tab. Permanent links show creator name, password status, creation date, and a live count of how many players have joined through each link. Delete individual invites or permanent links directly from the table.
- **Whitelist Management**: Full whitelist management with search, add, and remove functionality. Player names are parsed directly from the Velocity proxy's console output.
- **Single-Use Invites**: Generate standard one-time use tokens directly from the dashboard with an optional inviter name tag (labeled as `[Admin]`).
- **Permanent Links**: Generate reusable public invite URLs. Optionally secure them with a password. A confirmation dialog prevents accidental generation.
- **Tab Persistence**: The dashboard remembers your last active tab across page refreshes using `localStorage`.

### 🔐 Security First
- **One-time invite tokens** that expire after use.
- **Two-step password flow** for permanent links — if a link is password-protected, users must enter and verify the password against the server before they can see or interact with the username/CAPTCHA form.
- **hCaptcha protection** against automated abuse on all user-facing forms.
- **IP-based rate limiting** on all API endpoints (100 requests per 10 seconds).
- **CORS-compliant API** for secure cross-origin requests.
- **Secure dashboard authentication**: Password-protected dashboard with an internal SvelteKit proxy that hides admin credentials from client-side network requests.
- **Config preservation**: The plugin never overwrites an existing `config.yml` on restart.

## 🚀 Quick Start

### Prerequisites
- **Minecraft Server**: Velocity proxy (3.0+) with Java 17+.
- **Web Hosting**: Node.js 18+ environment for the frontend.
- **Domain**: For hosting the web interface.
- **hCaptcha Account**: For CAPTCHA protection ([free tier available](https://www.hcaptcha.com/)).

### Installation Overview

1. **Set up the Velocity Plugin**
   ```bash
   cd VelocityPlugin/
   mvn clean package
   # Copy target/TokenPassWhitelist-*.jar to your Velocity plugins/ folder
   ```

2. **Configure the Plugin (`config.yml`)**
   The plugin generates a `config.yml` in your `plugins/tokenpasswhitelist/` folder after the first run. It will **never overwrite** an existing config.
   ```yaml
   ip: 0.0.0.0
   port: 5000
   api_secret: "your-secure-secret"          # MUST match frontend AUTH_TOKEN
   admin_password: "your-admin-password"     # MUST match frontend ADMIN_PASSWORD
   website_domain: "your-domain.com"
   whitelist_command: "whitelist add "
   whitelist_list_command: "whitelist list"
   whitelist_remove_command: "whitelist remove "
   ```

   > ⚠️ **Important:** The `whitelist_command` and `whitelist_remove_command` values must have a **trailing space** — the username is appended directly to the string.

3. **Deploy the Frontend**
   The frontend requires a `.env` file in the `SvelteFrontend/` directory.
   ```bash
   cd SvelteFrontend/
   
   # Create a .env file (see SvelteFrontend/README.md for all variables)
   # Ensure AUTH_TOKEN and ADMIN_PASSWORD match your plugin's config.yml!
   
   npm install
   npm run build
   
   # Use PM2 to run the build continuously:
   npm install -g pm2
   pm2 start build/index.js --name "tokenpass-frontend"
   pm2 save && pm2 startup
   ```

## 🌍 Network & Proxy Setup Guide

Setting up the network correctly is the most important part of this project. The frontend needs to securely communicate with the backend API via your domain.

### 🌐 Single Domain Setup (Web + Minecraft)
Because Minecraft relies on a specific port (like `25565`) or SRV records, while web traffic uses ports `80` and `443`, you can use the **exact same domain** (e.g., `play.yourdomain.com`) for both the game server and the website!

- **Minecraft Traffic (Port 25565)** goes directly to your Velocity proxy.
- **Web Traffic (Port 80/443)** goes to your Reverse Proxy, which routes users to the frontend, and API calls to the Velocity plugin.

*(Note: If using Cloudflare to hide your IP, see the Cloudflare section below, as you may need a separate subdomain for the Minecraft server since Cloudflare's free tier only proxies web traffic).*

### 1. Backend IP Binding (`config.yml` → `ip`)
The IP you set in the Velocity plugin determines how the API server listens for connections:
- **`127.0.0.1` (Localhost)**: Use this if your reverse proxy (e.g., Nginx) runs on the **same machine** as the Minecraft server. This hides the API from the public internet entirely.
- **`0.0.0.0` (All Interfaces)**: Use this if you are running inside a **Docker container, Pterodactyl Wings, or Proxmox LXC**. This allows the API to bind to the container's network bridge so your reverse proxy (which is likely in a different container or on the host) can reach it. Make sure you allocate the API port (default `5000`) in your panel!

### 2. Reverse Proxy Configurations (Path-Based Routing)

To make the single domain setup work for both the website and the backend API, your reverse proxy uses **path-based routing**:
- Any request starting with `/api/` (e.g., `play.yourdomain.com/api/whitelist`) is routed to the Velocity plugin's port (default `5000`).
- Any other request (e.g., `play.yourdomain.com/admin`) is routed to the Node.js Svelte frontend port (default `3000`).

This setup completely avoids CORS errors and keeps your network clean.

#### Option A: Nginx Proxy Manager (NPM)
1. Add a **Proxy Host** for `your-domain.com`.
2. Under the **Details** tab, point the Forward Host/IP to your Frontend (Node.js) server IP and Port (e.g. `3000`).
3. Under the **Custom Locations** tab, add a new location:
   - **Location:** `/api/`
   - **Forward Host/IP:** Your Velocity server IP.
   - **Forward Port:** `5000` (or your configured plugin port).
4. Save and ensure SSL is enabled.

> 💡 **Tip for Single Domain Setups:** Nginx Proxy Manager only handles web traffic (ports 80/443). To make the Minecraft server work on the same domain, simply go to your router/firewall and **Port Forward 25565 directly to your Velocity proxy IP**. NPM and Velocity will live happily side-by-side using the same domain!

#### Option B: Traefik (Docker Compose)
If you run your frontend and proxy in Docker, add these labels to your frontend container:
```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.frontend.rule=Host(`your-domain.com`)"
  - "traefik.http.services.frontend.loadbalancer.server.port=3000"
  # Route /api/ to Velocity
  - "traefik.http.routers.backend.rule=Host(`your-domain.com`) && PathPrefix(`/api/`)"
  - "traefik.http.services.backend.loadbalancer.server.port=5000"
  - "traefik.http.services.backend.loadbalancer.server.url=http://VELOCITY_SERVER_IP:5000"
```

#### Option C: Standard Nginx
```nginx
server {
    listen 443 ssl;
    server_name your-domain.com;

    # Route /api/ to Velocity Plugin
    location /api/ {
        proxy_pass http://VELOCITY_IP:5000/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Route everything else to Svelte Frontend
    location / {
        proxy_pass http://FRONTEND_IP:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3. Cloudflare Integration

If you use Cloudflare for DNS, you must be aware of its port limitations. **Cloudflare's free tier only proxies web traffic (ports 80/443). It will block Minecraft traffic (port 25565).**

**Option A: The Single Domain Method (No IP Hiding)**
1. Set your `play.yourdomain.com` DNS record to **DNS Only (Gray Cloud)**.
2. Players can connect via `play.yourdomain.com`, and the website will work on `https://play.yourdomain.com`.

**Option B: The Subdomain Method (Hide Web IP)**
1. Create a record for `mc.yourdomain.com` and set it to **DNS Only (Gray Cloud)**. Give this to players to join the server.
2. Create a record for `www.yourdomain.com` (or the root domain) and set it to **Proxied (Orange Cloud)**.
3. **SSL/TLS Mode:** Set this to **Full (Strict)**. Ensure you have a valid SSL certificate on your reverse proxy (Let's Encrypt is perfect).
4. **Caching Rules:** The `/api/*` endpoints *must not be cached*. Go to Cloudflare Rules → Page Rules and create a rule:
   - **URL:** `your-domain.com/api/*`
   - **Setting:** Cache Level → Bypass

## 📁 Project Structure

```
TokenPassWhitelist/
├── README.md                        # This file
├── VelocityPlugin/
│   ├── README.md                    # Plugin-specific documentation
│   ├── pom.xml                      # Maven build file
│   └── src/main/
│       ├── java/.../
│       │   ├── TokenPassWhitelist.java    # Plugin entry point
│       │   ├── InternalHttpServer.java    # Built-in HTTP API (16 endpoints)
│       │   ├── InviteStorage.java         # Invite/permanent link persistence
│       │   ├── InviteCommand.java         # /invite command handler
│       │   └── ConfigFile.java            # Safe YAML config loader
│       └── resources/
│           └── config.yml                 # Default configuration template
└── SvelteFrontend/
    ├── README.md                    # Frontend-specific documentation
    ├── package.json                 # Node.js dependencies
    └── src/routes/
        ├── +page.svelte                    # Landing page
        ├── admin/                          # Admin login + dashboard
        ├── invite/[token]/                 # Single-use invite redemption
        ├── public-invite/[id]/             # Permanent link redemption page
        ├── validate-permanent-link/[id]/   # Permanent link validation proxy
        ├── verify-permanent-password/[id]/ # Password pre-verification proxy
        └── success/                        # Post-whitelist confirmation
```

## 🤝 Contributing

Contributions are welcome from all skill levels! Whether you're fixing a bug, adding a new feature, or improving documentation:

1. **Fork the repository** and create a feature branch.
2. **Follow the coding standards** established in each component.
3. **Submit a pull request** with a clear description of your changes.

If you have feature ideas or encounter bugs, please open an issue!

## 💖 Acknowledgements

Built with ❤️ for the Minecraft community by **guslof**.

*We stand on the shoulders of giants:*
- **[Velocity](https://velocitypowered.com/)** for the blazing fast proxy API.
- **[SvelteKit](https://kit.svelte.dev/)** for making frontend development a joy.
- **[vis-network](https://visjs.github.io/vis-network/)** for the beautiful invite tree visualization.
- **[hCaptcha](https://www.hcaptcha.com/)** for bot protection.

## 📄 License
This project is open-source and licensed under the MIT License — see the [LICENSE](LICENSE) file for details.