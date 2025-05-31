# TokenPassWhitelist - Secure Minecraft Whitelist System

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Velocity](https://img.shields.io/badge/Velocity-3.0+-blue.svg)](https://velocitypowered.com/)
[![SvelteKit](https://img.shields.io/badge/SvelteKit-2.x-red.svg)](https://kit.svelte.dev/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A modern, secure invite-based whitelisting system for Minecraft servers running on Velocity proxy. TokenPassWhitelist combines a robust server-side plugin with a sleek web frontend to provide a seamless invitation and registration experience.

## 🎯 What This Project Does

TokenPassWhitelist transforms the traditional Minecraft whitelist process into a secure, user-friendly system:

1. **Server administrators or trusted players** generate unique invite links using in-game commands or a web admin panel
2. **Invited users** click the link, complete CAPTCHA verification, and confirm their Minecraft username
3. **The system automatically** adds them to the server whitelist and notifies the inviter
4. **Everyone benefits** from a trusted community with verified members

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

- **[Velocity Plugin](TokenPassWhitelist/)** - Server-side Java plugin with HTTP API
- **[Web Frontend](frontend/)** - Modern SvelteKit application for invite management
- **Security Layer** - CAPTCHA verification, rate limiting, and token validation

## ✨ Key Features

### 🔐 Security First
- **One-time invite tokens** that expire after use
- **CAPTCHA protection** against automated abuse
- **Rate limiting** on all API endpoints
- **Admin password protection** for administrative functions
- **CORS-compliant API** for secure cross-origin requests

### 🎮 Player Experience
- **In-game invite generation** with `/invite` command
- **Clickable invite links** in chat with hover tooltips
- **Real-time notifications** when invites are redeemed
- **Invite tracking** with `/invite list` command
- **Mobile-friendly** web interface

### 👨‍💼 Administrative Control
- **Web admin panel** for creating invites externally
- **Persistent storage** with YAML-based data management
- **Configurable whitelist commands** for different server setups
- **Comprehensive logging** and error handling

### 🚀 Developer Friendly
- **RESTful HTTP API** for external integrations
- **TypeScript support** throughout the frontend
- **Modular architecture** for easy customization
- **Comprehensive documentation** and examples

## 🚀 Quick Start

### Prerequisites
- **Minecraft Server**: Velocity proxy (3.0+) with Java 17+
- **Web Hosting**: Node.js environment for the frontend
- **Domain**: For hosting the web interface (recommended)
- **hCaptcha Account**: For CAPTCHA protection (free tier available)

### Installation Overview

1. **Set up the Velocity Plugin**
   ```bash
   cd TokenPassWhitelist/
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
   ```

3. **Deploy the Frontend**
   ```bash
   cd frontend/
   npm install
   npm run build
   # Deploy the build/ directory to your web server
   ```

4. **Connect the Systems**
   - Configure your reverse proxy to route `/api/*` to the plugin
   - Set environment variables in your frontend deployment
   - Test the integration with health checks

📚 **Detailed Setup Instructions**: See component-specific README files for complete installation guides.

## 🔧 Configuration

### Environment Variables (Frontend)
```env
# Required for frontend functionality
VITE_BRAND_NAME="Your Server Name"
VITE_SERVER_IP="your-server.com"
VITE_API_URL="https://your-domain.com"
VITE_HCAPTCHA_SITE_KEY="your-site-key"

# Required for backend communication
AUTH_TOKEN="matches-plugin-api-secret"
ADMIN_PASSWORD="matches-plugin-admin-password"
HCAPTCHA_SECRET="your-secret-key"
```

### Plugin Configuration (config.yml)
```yaml
ip: 0.0.0.0                    # API server bind address
port: 5000                     # API server port
api_secret: REPLACE_ME         # Must match frontend AUTH_TOKEN
admin_password: change_me_now  # Must match frontend ADMIN_PASSWORD
website_domain: example.com    # Your frontend domain
whitelist_command: whitelist add  # Server whitelist command
```

## 🎮 Usage

### For Players
1. **Generate an invite**: Use `/invite` in-game to create a link for a friend
2. **Share the link**: Copy the clickable link from chat and send it
3. **Track your invites**: Use `/invite list` to see who has redeemed your invites

### For Administrators
1. **Access admin panel**: Visit `https://your-domain.com/admin`
2. **Create invites**: Generate links for specific usernames
3. **Monitor activity**: Check server logs and invite lists

### For Invited Users
1. **Click the invite link**: Opens the registration page
2. **Complete CAPTCHA**: Verify you're human
3. **Confirm username**: Your Minecraft username is pre-filled
4. **Join the server**: Get immediate whitelist access

## 📁 Project Structure

```
TokenPassWhitelistVelocityPlugin/
├── README.md                    # This file - project overview
├── TokenPassWhitelist/          # Velocity plugin (Java)
│   ├── README.md               # Plugin-specific documentation
│   ├── src/main/java/          # Plugin source code
│   ├── src/main/resources/     # Plugin configuration
│   └── pom.xml                 # Maven build configuration
└── frontend/                   # Web interface (SvelteKit)
    ├── README.md              # Frontend-specific documentation
    ├── src/                   # Frontend source code
    ├── package.json           # Node.js dependencies
    └── build/                 # Production build output
```

## 🔌 API Reference

The plugin exposes a RESTful HTTP API for frontend communication:

| Endpoint | Method | Purpose | Authentication |
|----------|--------|---------|----------------|
| `/api/whitelist` | POST | Redeem invite token | API Secret |
| `/api/check-token` | POST | Validate token | API Secret |
| `/api/invite-admin` | POST | Admin create invite | Admin Password |
| `/ping` | GET | Health check | None |

**Full API documentation**: See [TokenPassWhitelist/README.md](TokenPassWhitelist/README.md#-internal-http-api)

## 🛡️ Security Features

### Token Security
- **Cryptographically secure** token generation
- **One-time use** tokens that expire after redemption
- **UUID-based** invite tracking prevents guessing

### Rate Limiting
- **5 requests per 10 seconds** per IP address
- **Separate limits** for admin and user endpoints
- **Automatic cleanup** of expired rate limit entries

### Input Validation
- **Server-side validation** of all user inputs
- **Username sanitization** and format checking
- **CAPTCHA verification** on all user actions

### Network Security
- **CORS-compliant** API with proper headers
- **Authentication tokens** for all sensitive operations
- **HTTPS-ready** with secure cookie handling

## 🚀 Deployment

### Recommended Architecture

```
Internet → Reverse Proxy → Web Frontend
                ↓
            Minecraft Server ← Velocity Plugin
```

### Production Checklist

- [ ] **Change default passwords** in plugin configuration
- [ ] **Set up HTTPS** for the web frontend
- [ ] **Configure reverse proxy** for API routing
- [ ] **Set secure environment variables**
- [ ] **Test invite flow** end-to-end
- [ ] **Monitor server logs** for errors
- [ ] **Set up backups** for invite data

**Deployment Guides**: See component README files for detailed deployment instructions.

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **Fork the repository** and create a feature branch
2. **Follow the coding standards** established in each component
3. **Add tests** for new functionality where applicable
4. **Update documentation** for any API or configuration changes
5. **Submit a pull request** with a clear description

### Development Setup
```bash
# Clone the repository
git clone https://github.com/yourusername/TokenPassWhitelistVelocityPlugin.git

# Set up the plugin
cd TokenPassWhitelistVelocityPlugin/TokenPassWhitelist
mvn clean compile

# Set up the frontend
cd ../frontend
npm install
npm run dev
```

## 🐛 Troubleshooting

### Common Issues

**Plugin won't start**
- Verify Java 17+ and Velocity compatibility
- Check plugin logs for specific errors
- Ensure port is not already in use

**Frontend can't reach API**
- Verify `VITE_API_URL` configuration
- Check reverse proxy configuration
- Test API endpoints directly with curl

**Invites not working**
- Verify token generation in plugin logs
- Check CAPTCHA configuration
- Test network connectivity between components

**Rate limiting triggered**
- Wait for rate limit window to reset
- Check for multiple users behind same IP
- Consider adjusting limits in plugin code

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Velocity Team** for the excellent proxy platform
- **Svelte Team** for the modern web framework
- **hCaptcha** for free CAPTCHA services
- **Minecraft Community** for inspiration and feedback

## 📞 Support

- **Plugin Issues**: Check [TokenPassWhitelist/README.md](TokenPassWhitelist/README.md)
- **Frontend Issues**: Check [frontend/README.md](frontend/README.md)
- **General Questions**: Open an issue on GitHub
- **Security Concerns**: Email security@yourdomain.com

---

**Built with ❤️ for the Minecraft community**