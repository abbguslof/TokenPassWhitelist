# Minecraft Whitelist Frontend

A modern, secure web interface for managing Minecraft server whitelist invitations. Built with SvelteKit, TypeScript, and featuring robust security measures including rate limiting and CAPTCHA verification.

## 🎯 What This Application Does

This frontend provides a user-friendly web interface for:

- **Token-based Invitations**: Secure invite links for specific Minecraft usernames.
- **Permanent Links**: Publicly distributable links with optional password protection.
- **Admin Dashboard**: Visual invite tree, online players tracker, and full whitelist history.
- **CAPTCHA Protection**: Prevents automated abuse using hCaptcha.
- **Responsive Design**: Works seamlessly on desktop and mobile devices.

## 🏗️ Architecture

### Tech Stack
- **Framework**: SvelteKit 2.x with TypeScript
- **Styling**: Custom CSS with responsive design
- **Security**: hCaptcha integration, local session authentication
- **Deployment**: Node.js adapter for production builds

### Application Structure

```
src/
├── app.html              
├── app.d.ts              
├── lib/
│   ├── api.ts           
│   └── index.ts         
└── routes/
    ├── +page.svelte     
    ├── admin/
    │   ├── +page.svelte           # Admin login panel
    │   └── dashboard/             # Admin Dashboard feature
    │       ├── +page.svelte       # Tree, Whitelist, Links, etc.
    │       └── api/+server.ts     # Internal proxy for secure API calls
    ├── invite/
    │   └── [token]/
    │       ├── +page.svelte       # Single-use invite redemption page
    │       └── +server.ts         
    ├── public-invite/
    │   └── [id]/
    │       ├── +page.svelte       # Permanent link redemption page
    │       └── +server.ts         
    └── success/
        └── +page.svelte 
```

## 🚀 Getting Started

### Prerequisites

- Node.js 18+ 
- npm or yarn package manager
- Access to a Minecraft server with the TokenPassWhitelistVelocityPlugin

### Installation

1. **Clone and navigate to the frontend directory**
   ```bash
   cd SvelteFrontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure environment variables**
   Create a `.env` file in the root directory:
   ```env
   # Client-side variables (VITE_ prefix required)
   VITE_BRAND_NAME="Your Server Name"
   VITE_SERVER_IP="your-server.com"
   VITE_API_URL="https://your-domain.com"
   VITE_HCAPTCHA_SITE_KEY="your-hcaptcha-site-key"

   # Server-side variables (backend communication)
   AUTH_TOKEN="matches-plugin-api-secret"
   ADMIN_PASSWORD="matches-plugin-admin-password"
   HCAPTCHA_SECRET="your-hcaptcha-secret-key"
   ```

### Development

1. **Start the development server**
   ```bash
   npm run dev
   ```
   The application will be available at `http://localhost:5173`.

### Production Build & Deployment (PM2)

1. **Build the application**
   ```bash
   npm run build
   ```

2. **Deploy using PM2**
   PM2 is the recommended process manager for keeping your Node.js app alive.
   ```bash
   # Install PM2 globally if you haven't
   npm install -g pm2
   
   # Start the built app
   pm2 start build/index.js --name "tokenpass-frontend"
   
   # Save the PM2 list so it restarts on system reboot
   pm2 save
   pm2 startup
   ```

## 📚 Usage Guide

### For Administrators

1. **Access the admin panel** at `/admin`.
2. **Enter the admin password** (set in your `.env` file).
3. **Use the Dashboard**:
   - **🌳 Invite Tree**: See exactly how players are inviting each other via an interactive node graph.
   - **📋 Players & Whitelist**: See currently online players across the network and a history of redeemed invites.
   - **🎟️ Generate Invite**: Create a one-time use link for a specific username.
   - **🔗 Permanent Links**: Create a reusable link (e.g., for a Discord server) and optionally secure it with a password.

### For Users

1. **Click the invite link** provided by a friend or administrator.
2. **Complete the CAPTCHA** verification.
3. **Confirm the Minecraft username**.
4. **Submit the form** to be added to the whitelist instantly.

## 🛡️ Security Features

### Secure Admin Dashboard
The dashboard uses an internal proxy (`/admin/dashboard/api`) to relay your `ADMIN_PASSWORD` to the Java backend. Your password is never exposed in client-side XHR requests to the public API.

### CAPTCHA Protection
- **hCaptcha integration** on all user-facing forms.
- **Server-side verification** of CAPTCHA responses.

### Input Validation
- **Server-side validation** of all user inputs.
- **Token validation** before processing requests.
- **CSRF protection** built into SvelteKit.

## 🐛 Troubleshooting

**Backend connection errors**:
- Verify `VITE_API_URL` points to the root of your domain where the reverse proxy routes `/api/`.
- Ensure CORS is correctly handled by your reverse proxy or the Velocity plugin.

**CAPTCHA not loading**:
- Verify `VITE_HCAPTCHA_SITE_KEY` is correctly set.

**Rate limiting triggered**:
- Wait 1 minute before retrying. 

## 📄 License

This project is part of the TokenPassWhitelist ecosystem. Refer to the main project repository for licensing information.
