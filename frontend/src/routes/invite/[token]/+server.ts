import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

// Simple in-memory rate limiting
const rateLimitStore = new Map<string, { count: number; resetTime: number }>();
const RATE_LIMIT_WINDOW = 60000; // 1 minute
const MAX_REQUESTS_PER_WINDOW = 10; // 10 requests per minute per IP

function getRealClientIP(request: Request): string {
	// Check common proxy headers
	const forwarded = request.headers.get('x-forwarded-for');
	if (forwarded) {
		return forwarded.split(',')[0].trim();
	}
	
	const realIP = request.headers.get('x-real-ip');
	if (realIP) {
		return realIP;
	}
	
	// Fallback to a default if we can't determine IP
	return 'unknown';
}

function checkRateLimit(clientIP: string): boolean {
	const now = Date.now();
	const key = clientIP;
	
	// Clean up expired entries periodically
	if (Math.random() < 0.1) { // 10% chance to cleanup
		for (const [ip, data] of rateLimitStore.entries()) {
			if (now > data.resetTime) {
				rateLimitStore.delete(ip);
			}
		}
	}
	
	const current = rateLimitStore.get(key);
	
	if (!current || now > current.resetTime) {
		// First request or window expired
		rateLimitStore.set(key, { count: 1, resetTime: now + RATE_LIMIT_WINDOW });
		return true;
	}
	
	if (current.count >= MAX_REQUESTS_PER_WINDOW) {
		return false; // Rate limit exceeded
	}
	
	current.count++;
	return true;
}

export const POST: RequestHandler = async ({ request, params }) => {
	const clientIP = getRealClientIP(request);
	
	// Check rate limit
	if (!checkRateLimit(clientIP)) {
		return json({ message: 'Too many requests. Please wait before trying again.' }, { status: 429 });
	}

	const { username, captchaToken } = await request.json();
	const token = params.token;

	const API_URL = process.env.VITE_API_URL;
	const AUTH_TOKEN = process.env.AUTH_TOKEN;
	const HCAPTCHA_SECRET = process.env.HCAPTCHA_SECRET;

	// Check if environment variables are properly set
	if (!API_URL || !AUTH_TOKEN || !HCAPTCHA_SECRET) {
		return json({ message: 'Server configuration error' }, { status: 500 });
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

export const GET: RequestHandler = async ({ params, request }) => {
	const clientIP = getRealClientIP(request);
	
	// Check rate limit for token validation too
	if (!checkRateLimit(clientIP)) {
		return json({ message: 'Too many requests. Please wait before trying again.' }, { status: 429 });
	}

	const token = params.token;
	const API_URL = process.env.VITE_API_URL;
	const AUTH_TOKEN = process.env.AUTH_TOKEN;

	// Check if environment variables are properly set
	if (!API_URL || !AUTH_TOKEN) {
		return json({ message: 'Server configuration error' }, { status: 500 });
	}

	try {
		const res = await fetch(`${API_URL}/api/check-token`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'X-Auth-Token': AUTH_TOKEN
			},
			body: JSON.stringify({ token })
		});

		if (res.ok) {
			return json({ valid: true });
		} else {
			return json({ message: 'Invalid or expired token.' }, { status: 400 });
		}
	} catch (error) {
		return json({ message: 'Failed to validate token' }, { status: 500 });
	}
};
