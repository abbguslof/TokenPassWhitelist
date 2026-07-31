import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

import { env } from '$env/dynamic/private';

const API_URL = env.VITE_API_URL!;
const AUTH_TOKEN = env.AUTH_TOKEN!;
const HCAPTCHA_SECRET = env.HCAPTCHA_SECRET!;

export const POST: RequestHandler = async ({ request, params }) => {
    const { username, captchaToken } = await request.json();
    const token = params.token;

    // Verify hCaptcha
    const verify = await fetch('https://hcaptcha.com/siteverify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
            secret: HCAPTCHA_SECRET,
            response: captchaToken
        })
    });
    const captchaResult = await verify.json();
    if (!captchaResult.success) {
        return json({ message: 'CAPTCHA failed.' }, { status: 400 });
    }

    // Forward to Velocity plugin
    const res = await fetch(`${API_URL}/api/whitelist`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Auth-Token': AUTH_TOKEN
        },
        body: JSON.stringify({ token, username })
    });

    const result = await res.json();
    if (!res.ok) {
        return json({ message: result.message || 'Server error' }, { status: res.status });
    }

    return json({ message: result.message });
};