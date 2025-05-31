import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

export const POST: RequestHandler = async ({ request, params }) => {
	const { username, captchaToken } = await request.json();
	const token = params.token;

	const API_URL = process.env.VITE_API_URL;
	const AUTH_TOKEN = process.env.AUTH_TOKEN;
	const HCAPTCHA_SECRET = process.env.HCAPTCHA_SECRET;

	// Check if environment variables are properly set
	if (!API_URL || !AUTH_TOKEN || !HCAPTCHA_SECRET) {
		console.error('Missing environment variables:', { API_URL, AUTH_TOKEN: !!AUTH_TOKEN, HCAPTCHA_SECRET: !!HCAPTCHA_SECRET });
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

export const GET: RequestHandler = async ({ params }) => {
	const token = params.token;
	const API_URL = process.env.VITE_API_URL;
	const AUTH_TOKEN = process.env.AUTH_TOKEN;

	console.log('=== Token Validation Debug ===');
	console.log('Token to validate:', token);
	console.log('API_URL:', API_URL);
	console.log('AUTH_TOKEN:', AUTH_TOKEN ? 'SET' : 'MISSING');

	// Check if environment variables are properly set
	if (!API_URL || !AUTH_TOKEN) {
		console.error('Missing environment variables for token check:', { API_URL, AUTH_TOKEN: !!AUTH_TOKEN });
		return json({ message: 'Server configuration error' }, { status: 500 });
	}

	try {
		console.log('Making request to:', `${API_URL}/api/check-token`);
		console.log('With headers:', {
			'Content-Type': 'application/json',
			'X-Auth-Token': AUTH_TOKEN
		});
		console.log('With body:', JSON.stringify({ token }));

		const res = await fetch(`${API_URL}/api/check-token`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'X-Auth-Token': AUTH_TOKEN
			},
			body: JSON.stringify({ token })
		});

		console.log('Response status:', res.status);
		const responseText = await res.text();
		console.log('Response body:', responseText);

		if (res.ok) {
			return json({ valid: true });
		} else {
			return json({ message: 'Invalid or expired token.' }, { status: 400 });
		}
	} catch (error) {
		console.error('Error checking token:', error);
		return json({ message: 'Failed to validate token' }, { status: 500 });
	}
};
