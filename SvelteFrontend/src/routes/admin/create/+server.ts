import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { env } from '$env/dynamic/private';

// Simple in-memory rate limiting
const rateLimitStore = new Map<string, { count: number; resetTime: number }>();
const RATE_LIMIT_WINDOW = 60000; // 1 minute
const MAX_REQUESTS_PER_WINDOW = 5; // 5 admin requests per minute per IP (stricter than regular endpoints)

function getRealClientIP(request: Request): string {
	const forwarded = request.headers.get('x-forwarded-for');
	if (forwarded) {
		return forwarded.split(',')[0].trim();
	}
	
	const realIP = request.headers.get('x-real-ip');
	if (realIP) {
		return realIP;
	}
	
	return 'unknown';
}

function checkRateLimit(clientIP: string): boolean {
	const now = Date.now();
	const key = `admin-${clientIP}`;
	
	// Clean up expired entries periodically
	if (Math.random() < 0.1) {
		for (const [ip, data] of rateLimitStore.entries()) {
			if (now > data.resetTime) {
				rateLimitStore.delete(ip);
			}
		}
	}
	
	const current = rateLimitStore.get(key);
	
	if (!current || now > current.resetTime) {
		rateLimitStore.set(key, { count: 1, resetTime: now + RATE_LIMIT_WINDOW });
		return true;
	}
	
	if (current.count >= MAX_REQUESTS_PER_WINDOW) {
		return false;
	}
	
	current.count++;
	return true;
}

export const POST: RequestHandler = async ({ request }) => {
	const clientIP = getRealClientIP(request);
	
	// Check rate limit
	if (!checkRateLimit(clientIP)) {
		return json({ message: 'Too many admin requests. Please wait before trying again.' }, { status: 429 });
	}

	const { password, username, inviter } = await request.json();

	const ADMIN_PASSWORD = env.ADMIN_PASSWORD;
	const API_URL = env.VITE_API_URL;

	// Check if environment variables are properly set
	if (!ADMIN_PASSWORD || !API_URL) {
		console.error('Missing environment variables:', { ADMIN_PASSWORD: !!ADMIN_PASSWORD, API_URL });
		return json({ message: 'Server configuration error' }, { status: 500 });
	}

	if (!password || password !== ADMIN_PASSWORD) {
		return json({ message: 'Unauthorized' }, { status: 401 });
	}

	try {
		const res = await fetch(`${API_URL}/api/invite-admin`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'X-Admin-Password': ADMIN_PASSWORD
			},
			body: JSON.stringify({
				username,
				inviterName: inviter || 'WebAdmin'
			})
		});

		const data = await res.json();
		if (!res.ok) {
			return json({ message: data.message || 'Failed to create invite' }, { status: res.status });
		}

		return json({ token: data.token, link: data.link });
	} catch (error) {
		console.error('Error creating admin invite:', error);
		return json({ message: 'Failed to create invite' }, { status: 500 });
	}
};
