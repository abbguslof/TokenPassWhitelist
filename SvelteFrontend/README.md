# Minecraft Whitelist Frontend

A modern, secure web interface for managing Minecraft server whitelist invitations. Built with SvelteKit, TypeScript, and featuring robust security measures including rate limiting and CAPTCHA verification.

## 🎯 What This Application Does

This frontend provides a user-friendly web interface for:

- **Token-based Invitations**: Generate secure invite links for specific Minecraft usernames
- **Admin Panel**: Create and manage whitelist invitations with password protection
- **CAPTCHA Protection**: Prevents automated abuse using hCaptcha
- **Rate Limiting**: Built-in protection against spam and abuse
- **Responsive Design**: Works seamlessly on desktop and mobile devices

## 🏗️ Architecture

### Tech Stack
- **Framework**: SvelteKit 2.x with TypeScript
- **Styling**: Custom CSS with responsive design
- **Security**: hCaptcha integration, rate limiting, CSRF protection
- **Deployment**: Node.js adapter for production builds

### Application Structure

```
src/
├── app.html              # Main HTML template
├── app.d.ts              # TypeScript global declarations
├── lib/
│   ├── api.ts           # API utility functions
│   └── index.ts         # Library exports
└── routes/
    ├── +page.svelte     # Home page (currently empty)
    ├── admin/
    │   ├── +page.svelte # Admin panel for creating invites
    │   └── create/
    │       └── +server.ts # Admin invite creation endpoint
    ├── invite/
    │   └── [token]/
    │       ├── +page.svelte # Invite redemption page
    │       ├── +server.ts   # Token validation & processing
    │       └── whitelist/
    │           └── +server.ts # Legacy whitelist endpoint
    └── success/
        └── +page.svelte # Success confirmation page
```

## 🚀 Getting Started

### Prerequisites

- Node.js 18+ 
- npm or yarn package manager
- Access to a Minecraft server with the TokenPassWhitelistVelocityPlugin

### Installation

1. **Clone and navigate to the frontend directory**
   ```bash
   cd frontend
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
   VITE_API_URL="https://your-api-endpoint.com"
   VITE_HCAPTCHA_SITE_KEY="your-hcaptcha-site-key"

   # Server-side variables (backend communication)
   AUTH_TOKEN="your-auth-token-for-backend"
   ADMIN_PASSWORD="your-admin-password"
   HCAPTCHA_SECRET="your-hcaptcha-secret-key"
   ```

### Development

1. **Start the development server**
   ```bash
   npm run dev
   ```
   The application will be available at `http://localhost:5173`

2. **Type checking**
   ```bash
   npm run check
   ```

3. **Continuous type checking**
   ```bash
   npm run check:watch
   ```

### Production Build

1. **Build the application**
   ```bash
   npm run build
   ```

2. **Preview the production build**
   ```bash
   npm run preview
   ```

3. **Deploy the built application**
   
   The build output will be in the `build/` directory, ready for deployment on any Node.js hosting platform.

## 🔧 Configuration

### Environment Variables

| Variable | Type | Description |
|----------|------|-------------|
| `VITE_BRAND_NAME` | Client | Server/brand name displayed in UI |
| `VITE_SERVER_IP` | Client | Minecraft server IP address |
| `VITE_API_URL` | Client | Backend API endpoint URL |
| `VITE_HCAPTCHA_SITE_KEY` | Client | hCaptcha site key for frontend |
| `AUTH_TOKEN` | Server | Authentication token for backend API |
| `ADMIN_PASSWORD` | Server | Password for admin panel access |
| `HCAPTCHA_SECRET` | Server | hCaptcha secret key for verification |

### Server Configuration

The application runs on:
- **Host**: `0.0.0.0` (configurable via `HOST` env var)
- **Port**: `5173` (dev) / `3000` (production, configurable via `PORT` env var)

## 📚 Usage Guide

### For Administrators

1. **Access the admin panel** at `/admin`
2. **Enter the admin password** (set in environment variables)
3. **Create invite links** by providing:
   - Target Minecraft username
   - Optional inviter name
4. **Share the generated link** with the intended recipient

### For Users

1. **Click the invite link** provided by an administrator
2. **Complete the CAPTCHA** verification
3. **Confirm the Minecraft username** (pre-filled from invite)
4. **Submit the form** to be added to the whitelist
5. **View success page** with server connection details

## 🛡️ Security Features

### Rate Limiting
- **General endpoints**: 10 requests per minute per IP
- **Admin endpoints**: 5 requests per minute per IP (stricter)
- **Automatic cleanup** of expired rate limit entries

### CAPTCHA Protection
- **hCaptcha integration** on all user-facing forms
- **Server-side verification** of CAPTCHA responses
- **Prevents automated abuse** and bot attacks

### Input Validation
- **Server-side validation** of all user inputs
- **Token validation** before processing requests
- **CSRF protection** built into SvelteKit

## 🎨 Customization

### Styling
The application uses custom CSS with CSS variables for easy theming. Key files:
- Component styles are scoped within each `.svelte` file
- Responsive design with mobile-first approach
- Dark/light theme support where applicable

### Branding
Update the following in your `.env` file:
```env
VITE_BRAND_NAME="Your Custom Server Name"
VITE_SERVER_IP="your-server.example.com"
```

### UI Components
Each page component is self-contained with its own styles:
- `src/routes/admin/+page.svelte` - Admin panel
- `src/routes/invite/[token]/+page.svelte` - Invite form
- `src/routes/success/+page.svelte` - Success page

## 🔌 API Integration

### Backend Requirements
The frontend expects a backend API with these endpoints:

- `POST /api/invite-admin` - Create admin invites
- `POST /api/check-token` - Validate invite tokens
- `POST /api/whitelist` - Process whitelist requests

### Authentication
- Uses `X-Auth-Token` header for API authentication
- Admin endpoints use `X-Admin-Password` header

## 🚀 Deployment

### Node.js Production
1. Build the application: `npm run build`
2. Deploy the `build/` directory to your Node.js server
3. Set production environment variables
4. Start with: `node build/index.js`

### Adapter Configuration
The app uses `@sveltejs/adapter-node` for Node.js deployment. For other platforms:

1. **Install appropriate adapter**:
   ```bash
   npm install @sveltejs/adapter-static  # For static hosting
   npm install @sveltejs/adapter-vercel  # For Vercel
   ```

2. **Update `svelte.config.js`**:
   ```javascript
   import adapter from '@sveltejs/adapter-static';
   // or your chosen adapter
   ```

## 🐛 Troubleshooting

### Common Issues

**CAPTCHA not loading**:
- Verify `VITE_HCAPTCHA_SITE_KEY` is correctly set
- Check browser console for JavaScript errors
- Ensure hCaptcha script is not blocked by ad blockers

**Backend connection errors**:
- Verify `VITE_API_URL` points to your backend
- Check `AUTH_TOKEN` matches backend configuration
- Ensure CORS is properly configured on backend

**Rate limiting triggered**:
- Wait 1 minute before retrying
- Check if multiple users share the same IP
- Consider adjusting rate limits in server code

**Build errors**:
- Run `npm run check` to identify TypeScript issues
- Ensure all environment variables are properly set
- Clear `.svelte-kit` directory and rebuild

## 📄 License

This project is part of the TokenPassWhitelistVelocityPlugin ecosystem. Refer to the main project repository for licensing information.

## 🤝 Contributing

1. Follow the existing code style and conventions
2. Add TypeScript types for new functionality
3. Test changes in both development and production builds
4. Update this README for any new configuration options

## 📞 Support

For issues related to:
- **Frontend bugs**: Check browser console and network tab
- **Backend integration**: Verify API endpoints and authentication
- **Deployment**: Check server logs and environment configuration

Refer to the main project repository for additional documentation and support.
