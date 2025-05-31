// vite.config.ts
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');
    
    return {
        plugins: [sveltekit()],
        server: {
            host: '0.0.0.0',
            port: 5173
        },
        define: {
            // Only expose specific VITE_ prefixed variables to client
            'import.meta.env.VITE_BRAND_NAME': JSON.stringify(env.VITE_BRAND_NAME),
            'import.meta.env.VITE_SERVER_IP': JSON.stringify(env.VITE_SERVER_IP),
            'import.meta.env.VITE_API_URL': JSON.stringify(env.VITE_API_URL),
            'import.meta.env.VITE_HCAPTCHA_SITE_KEY': JSON.stringify(env.VITE_HCAPTCHA_SITE_KEY)
        }
    };
});