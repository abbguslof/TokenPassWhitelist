import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { env } from '$env/dynamic/private';

const rateLimitStore = new Map<string, { count: number; resetTime: number }>();
const RATE_LIMIT_WINDOW = 60000;
const MAX_REQUESTS_PER_WINDOW = 20;

function getRealClientIP(request: Request): string {
	const forwarded = request.headers.get('x-forwarded-for');
	if (forwarded) return forwarded.split(',')[0].trim();
	const realIP = request.headers.get('x-real-ip');
	if (realIP) return realIP;
	return 'unknown';
}

function checkRateLimit(clientIP: string): boolean {
	const now = Date.now();
	
	if (Math.random() < 0.1) {
		for (const [ip, data] of rateLimitStore.entries()) {
			if (now > data.resetTime) {
				rateLimitStore.delete(ip);
			}
		}
	}
	
	const current = rateLimitStore.get(clientIP);

	if (!current || now > current.resetTime) {
		rateLimitStore.set(clientIP, { count: 1, resetTime: now + RATE_LIMIT_WINDOW });
		return true;
	}

	if (current.count >= MAX_REQUESTS_PER_WINDOW) return false;
	current.count++;
	return true;
}

export const GET: RequestHandler = async ({ params, request }) => {
	const clientIP = getRealClientIP(request);
	if (!checkRateLimit(clientIP)) return json({ message: 'Too many requests' }, { status: 429 });

	const token = params.token;
	const API_URL = env.VITE_API_URL;
	const AUTH_TOKEN = env.AUTH_TOKEN;

	if (!API_URL || !AUTH_TOKEN) return json({ message: 'Server config error' }, { status: 500 });

	try {
		const res = await fetch(`${API_URL}/api/check-token`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'X-Auth-Token': AUTH_TOKEN
			},
			body: JSON.stringify({ token })
		});

		if (res.ok) return json({ valid: true });
		else return json({ message: 'Invalid or expired token' }, { status: 400 });
	} catch {
		return json({ message: 'Failed to validate token' }, { status: 500 });
	}
};

export const POST: RequestHandler = async ({ request, params }) => {
	const clientIP = getRealClientIP(request);
	if (!checkRateLimit(clientIP)) return json({ message: 'Too many requests' }, { status: 429 });

	const { username, captchaToken } = await request.json();
	const token = params.token;

	if (!username || !/^[a-zA-Z0-9_]{3,16}$/.test(username)) {
		return json({ message: 'Invalid username format' }, { status: 400 });
	}

	const API_URL = env.VITE_API_URL;
	const AUTH_TOKEN = env.AUTH_TOKEN;
	const HCAPTCHA_SECRET = env.HCAPTCHA_SECRET;

	if (!API_URL || !AUTH_TOKEN || !HCAPTCHA_SECRET) {
		return json({ message: 'Server config error' }, { status: 500 });
	}

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
		return json({ message: 'CAPTCHA failed' }, { status: 400 });
	}

	// Send to Velocity backend
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
