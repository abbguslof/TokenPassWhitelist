// vite.config.ts
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import { config } from 'dotenv';

// Load environment variables from .env file
config();

export default defineConfig({
	plugins: [sveltekit()],
	server: {
		host: '0.0.0.0',
		port: 5173
	},
	define: {
		// Only expose specific VITE_ prefixed variables to client
		'import.meta.env.VITE_BRAND_NAME': JSON.stringify(process.env.VITE_BRAND_NAME),
		'import.meta.env.VITE_SERVER_IP': JSON.stringify(process.env.VITE_SERVER_IP),
		'import.meta.env.VITE_HCAPTCHA_SITE_KEY': JSON.stringify(process.env.VITE_HCAPTCHA_SITE_KEY)
	}
});