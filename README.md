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
4. **Everyone benefits** from a trusted community with verified members.

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

- **[Velocity Plugin](VelocityPlugin/)** - Server-side Java plugin with HTTP API.
- **[Web Frontend](SvelteFrontend/)** - Modern SvelteKit application for invite management.
- **Security Layer** - CAPTCHA verification, rate limiting, and token validation.

## ✨ Key Features

### 🎮 Player Experience
- **In-game invite generation** with `/invite` command.
- **Clickable invite links** in chat with hover tooltips.
- **Real-time notifications** when invites are redeemed.
- **Invite tracking** with `/invite list` command.
- **Mobile-friendly** web interface.

### 👨‍💼 Comprehensive Admin Dashboard
- **Interactive Invite Tree**: Visualize who invited who with an interactive, draggable network graph built with `vis-network`.
- **Live Online Players Tracker**: See exactly who is online across the proxy network in real-time.
- **Whitelist History**: See a full history of all players whitelisted via the plugin, their inviting party, and the date they joined.
- **Permanent Links**: Generate permanent public invite URLs. Optionally secure them with a password to prevent abuse.
- **Single-Use Invites**: Generate standard one-time use tokens directly from the dashboard.

### 🔐 Security First
- **One-time invite tokens** that expire after use.
- **CAPTCHA protection** against automated abuse.
- **Rate limiting** on all API endpoints.
- **CORS-compliant API** for secure cross-origin requests.
- **Secure Dashboard Authentication**: Password-protected dashboard with token-based internal routing.

## 🚀 Quick Start

### Prerequisites
- **Minecraft Server**: Velocity proxy (3.0+) with Java 17+.
- **Web Hosting**: Node.js environment for the frontend.
- **Domain**: For hosting the web interface.
- **hCaptcha Account**: For CAPTCHA protection (free tier available).

### Installation Overview

1. **Set up the Velocity Plugin**
   ```bash
   cd VelocityPlugin/
   mvn clean package
   # Copy target/TokenPassWhitelist-*.jar to your Velocity plugins/ folder
   ```

2. **Configure the Plugin**
   ```yaml
   # Edit config.yml after first run
   ip: 0.0.0.0
   port: 5000
   api_secret: your-secure-secret
   admin_password: your-admin-password
   website_domain: your-domain.com
   whitelist_command: whitelist add
   ```

3. **Deploy the Frontend**
   ```bash
   cd SvelteFrontend/
   npm install
   npm run build
   # Use PM2 to run the build continuously:
   npm install -g pm2
   pm2 start build/index.js --name "tokenpass-frontend"
   ```

## 🌍 Network & Proxy Setup Guide

Setting up the network correctly is the most important part of this project. The frontend needs to securely communicate with the backend API via your domain.

### 🌐 Single Domain Setup (Web + Minecraft)
Because Minecraft relies on a specific port (like `25565`) or SRV records, while web traffic uses ports `80` and `443`, you can use the **exact same domain** (e.g., `play.yourdomain.com`) for both the game server and the website!

- **Minecraft Traffic (Port 25565)** goes directly to your Velocity proxy.
- **Web Traffic (Port 80/443)** goes to your Reverse Proxy, which routes users to the frontend, and API calls to the Velocity plugin.

*(Note: If using Cloudflare to hide your IP, see the Cloudflare section below, as you may need a separate subdomain for the Minecraft server since Cloudflare's free tier only proxies web traffic).*

### 1. Backend IP Binding (`config.yml` -> `ip`)
The IP you set in the Velocity plugin determines how the API server listens for connections:
- **`127.0.0.1` (Localhost)**: Use this if you are running a **Baremetal/VPS** setup and your Reverse Proxy (e.g. Nginx) is running on the *same machine*. This ensures the API is completely hidden from the public web and can only be accessed through the proxy.
- **`0.0.0.0` (All Interfaces)**: Use this if you are running inside a **Docker Container, Pterodactyl Wings, or Proxmox LXC**. This allows the API to bind to the container's network bridge so your reverse proxy (which is likely in a different container or on the host) can reach it. Make sure you allocate the API port (default `5000`) in your panel!

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

If you use Cloudflare for DNS, follow these crucial steps to ensure the application functions correctly:
1. **Proxy Status:** You can safely set your DNS record to **Proxied (Orange Cloud)**.
2. **SSL/TLS Mode:** Set this to **Full (Strict)**. Ensure you have a valid SSL certificate on your reverse proxy (Let's Encrypt is perfect).
3. **Caching Rules:** The `/api/*` endpoints *must not be cached*. Go to Cloudflare Rules -> Page Rules and create a rule:
   - **URL:** `your-domain.com/api/*`
   - **Setting:** Cache Level -> Bypass
   *(Note: The Velocity API sets `Cache-Control: no-cache` by default, but this rule guarantees Cloudflare won't interfere).*

## 📁 Project Structure

```
VelocityPlugin/
├── README.md                    # Plugin-specific documentation
├── src/main/java/               # Java 17+ source code
└── src/main/resources/          # Plugin configurations
SvelteFrontend/
├── README.md                    # Frontend-specific documentation
├── src/                         # SvelteKit source code
└── package.json                 # Node.js dependencies
```

## 🤝 Contributing

This project is built for the community, and we welcome contributions from all skill levels! Whether you're fixing a bug, adding a new feature, or simply correcting a typo in the documentation, your help is appreciated. 

Here's how you can help:
1. **Fork the repository** and create a feature branch.
2. **Follow the coding standards** established in each component.
3. **Submit a pull request** with a clear description of your changes.

If you have feature ideas or encounter bugs, please open an issue!

## 💖 Acknowledgements

Built with ❤️ for the Minecraft community by **guslof**.

A huge thank you to the server admins, community members, and players who provided feedback, tested early versions, and requested features like the Admin Dashboard and Invite Trees. This project exists to make running a safe, fun, and private Minecraft server as easy as possible for everyone.

*We stand on the shoulders of giants:*
- **Velocity Powered** for the blazing fast proxy API.
- **SvelteKit** for making frontend development a joy.
- **vis-network** for the beautiful invite tree visualization.

## 📄 License
This project is open-source and licensed under the MIT License - see the [LICENSE](LICENSE) file for details. You are free to use, modify, and distribute this software as you see fit for your communities.