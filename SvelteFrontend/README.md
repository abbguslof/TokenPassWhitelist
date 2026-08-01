# TokenPassWhitelist — Web Frontend

A modern, secure web interface for managing Minecraft server whitelist invitations. Built with SvelteKit and TypeScript, featuring an interactive admin dashboard, CAPTCHA-protected invite forms, and a two-step password flow for permanent links.

> ✅ Part of the larger **TokenPassWhitelist** system, which includes a [Velocity plugin](../VelocityPlugin/) that provides the backend HTTP API.

## 🎯 What This Application Does

This frontend provides a user-friendly web interface for:

- **Token-based Invitations**: Secure, one-time-use invite links for Minecraft usernames.
- **Permanent Links**: Publicly distributable invite URLs with optional password protection and a secure two-step verification flow.
- **Admin Dashboard**: Interactive invite tree, whitelist management, online player tracking, permanent link monitoring, and invite generation.
- **CAPTCHA Protection**: Prevents automated abuse using hCaptcha on all user-facing forms.
- **Responsive Design**: Works on desktop and mobile devices.

## 🏗️ Architecture

### Tech Stack
- **Framework**: SvelteKit 2.x with TypeScript
- **Visualization**: vis-network for the invite tree graph
- **Security**: hCaptcha integration, internal API proxy, `localStorage` session management
- **Deployment**: Node.js adapter for production builds (PM2 recommended)

### Route Structure

```
src/routes/
├── +page.svelte                          # Landing page
├── admin/
│   ├── +page.svelte                      # Admin login page
│   ├── create/+server.ts                 # Legacy invite creation proxy
│   └── dashboard/
│       ├── +page.svelte                  # Full admin dashboard (tree, invites, whitelist, links)
│       └── api/+server.ts               # Internal proxy — relays admin requests to Java backend
├── api/
│   └── public-invite/[id]/+server.ts     # Permanent link API proxy (GET info, POST redeem)
├── invite/
│   └── [token]/
│       ├── +page.svelte                  # One-time invite redemption page
│       └── +server.ts                    # Token validation & whitelist proxy
├── public-invite/
│   └── [id]/+page.svelte                # Permanent link redemption page (two-step password flow)
└── success/
    └── +page.svelte                      # Post-whitelist confirmation page
```

## 🚀 Getting Started

### Prerequisites

- Node.js 18+
- npm
- Access to a Minecraft server running the [TokenPassWhitelist Velocity plugin](../VelocityPlugin/)
- An [hCaptcha](https://www.hcaptcha.com/) account (free tier works fine)

### Installation

```bash
cd SvelteFrontend
npm install
```

### Environment Variables

Create a `.env` file in this directory:

```env
# Client-side variables (VITE_ prefix makes them available in browser code)
VITE_BRAND_NAME="Your Server Name"
VITE_SERVER_IP="play.yourdomain.com"
VITE_API_URL="https://yourdomain.com"
VITE_HCAPTCHA_SITE_KEY="your-hcaptcha-site-key"

# Server-side variables (never exposed to the browser)
AUTH_TOKEN="must-match-plugin-api_secret"
ADMIN_PASSWORD="must-match-plugin-admin_password"
HCAPTCHA_SECRET="your-hcaptcha-secret-key"
```

> ⚠️ **Critical:** `AUTH_TOKEN` must exactly match the `api_secret` in the plugin's `config.yml`, and `ADMIN_PASSWORD` must exactly match the `admin_password` in the plugin's `config.yml`. Mismatches will cause `401 Unauthorized` errors.

> ⚠️ **`VITE_API_URL`** should point to the **root** of the domain where your reverse proxy routes `/api/` requests to the Velocity plugin. For example, if your domain is `https://play.myserver.com` and your Nginx routes `play.myserver.com/api/*` to the plugin, then set `VITE_API_URL=https://play.myserver.com`.

### Development

```bash
npm run dev
```

The application will be available at `http://localhost:5173`.

### Production Build & Deployment

```bash
# Build
npm run build

# Deploy with PM2 (recommended)
npm install -g pm2
pm2 start build/index.js --name "tokenpass-frontend"
pm2 save
pm2 startup
```

The production server runs on port `3000` by default.

## 📚 Usage Guide

### For Administrators

1. Navigate to `/admin` and enter the admin password.
2. Use the sidebar to navigate between dashboard tabs:

| Tab | Description |
|-----|-------------|
| 🌳 **Invite Tree** | Interactive graph showing the full invite chain. Green nodes = whitelisted, red = removed from whitelist, pink = pending. Click any node to select it and optionally delete its invite record. A color legend is displayed above the graph. |
| 🎫 **Active Invites** | Two tables: **Permanent Links** (with creator, password status, uses count, and delete button) and **Unclaimed Invites** (with inviter, date, and delete button). |
| 📋 **Players & Whitelist** | Full whitelist management. Displays player count, search bar, individual remove buttons, and an add player form. Raw console output is available in a collapsible debug section. |
| 🎟️ **Generate Invite** | Create a single-use invite link with an optional inviter name tag (shown as `[Admin] Name` in the tree). |
| 🔗 **Permanent Links** | Generate reusable invite URLs with an optional password. A confirmation dialog prevents accidental generation. |

3. Click **🚪 Logout** to clear the saved session and return to the login page.

### For Users (One-Time Invites)

1. Click the invite link shared by a friend or administrator.
2. Complete the hCaptcha verification.
3. Enter your Minecraft username.
4. Submit the form to be added to the whitelist instantly.

### For Users (Permanent Links)

1. Click the permanent link.
2. **If password-protected:** Enter the password first. The username field and CAPTCHA are hidden until the password is verified.
3. **Once past the password step:** Enter your Minecraft username and complete the CAPTCHA.
4. Submit the form to be added to the whitelist.

## 🛡️ Security Features

### Internal Admin Proxy
The dashboard uses an internal SvelteKit API route (`/admin/dashboard/api`) to proxy all admin requests. The `ADMIN_PASSWORD` is sent server-side from the Node.js process to the Java backend — it is **never exposed** in client-side network requests visible in browser DevTools.

### Two-Step Permanent Link Flow
If a permanent link has a password, the user sees only a password input field on the first step. The username input, CAPTCHA widget, and submit button are completely hidden until the password step is completed. The password is held in memory and submitted alongside the username in a single POST to the backend.

### CAPTCHA Protection
- hCaptcha is integrated on all user-facing redemption forms (both one-time and permanent).
- CAPTCHA responses are verified server-side before any whitelist action is taken.

### Input Validation
- Username validation regex: `^[a-zA-Z0-9_.*-]{3,24}$` (supports Java and Bedrock/Geyser usernames).
- Server-side validation on both the SvelteKit proxy and the Java backend.
- Whitespace is automatically trimmed from usernames.

## 🐛 Troubleshooting

**"Failed to contact backend server":**
- Verify `VITE_API_URL` in `.env` points to the root of your domain.
- Ensure your reverse proxy correctly routes `/api/*` to the Velocity plugin's port.

**"Invalid admin password":**
- Ensure `ADMIN_PASSWORD` in `.env` exactly matches `admin_password` in the plugin's `config.yml`.

**"Failed to validate link" on permanent links:**
- Ensure the SvelteKit app has been rebuilt and redeployed after code changes.
- Verify `AUTH_TOKEN` in `.env` matches `api_secret` in the plugin's `config.yml`.

**CAPTCHA not loading:**
- Verify `VITE_HCAPTCHA_SITE_KEY` is set correctly in `.env`.
- Ensure the hCaptcha script (`https://js.hcaptcha.com/1/api.js`) is not blocked by an ad blocker or CSP.

**"Please match the requested format" on username input:**
- This is a browser HTML5 validation message. The pattern accepts `a-z`, `A-Z`, `0-9`, `_`, `.`, `*`, and `-` (3-24 characters). If a valid username is rejected, check for invisible whitespace characters.

## 📄 License

This project is part of the TokenPassWhitelist ecosystem. See the [LICENSE](../LICENSE) file in the root of the repository.
