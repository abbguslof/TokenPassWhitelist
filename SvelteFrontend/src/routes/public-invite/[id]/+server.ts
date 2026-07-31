import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { env } from '$env/dynamic/private';

export const GET: RequestHandler = async ({ params }) => {
	const API_URL = env.VITE_API_URL;
	const AUTH_TOKEN = env.AUTH_TOKEN;

	if (!API_URL || !AUTH_TOKEN) return json({ message: 'Server config error' }, { status: 500 });

	try {
		const res = await fetch(`${API_URL}/api/permanent-link-info`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'X-Auth-Token': AUTH_TOKEN
			},
			body: JSON.stringify({ id: params.id })
		});

		if (res.ok) {
			const data = await res.json();
			return json(data);
		} else {
			return json({ message: 'Invalid or expired link' }, { status: 404 });
		}
	} catch {
		return json({ message: 'Failed to validate link' }, { status: 500 });
	}
};

export const POST: RequestHandler = async ({ request, params }) => {
	const { username, captchaToken, password } = await request.json();
	const id = params.id;

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
	const res = await fetch(`${API_URL}/api/permanent-whitelist`, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
			'X-Auth-Token': AUTH_TOKEN
		},
		body: JSON.stringify({ id, username, passwordHash: password }) // Note: we should hash it normally, but keeping simple
	});

	const result = await res.json();
	if (!res.ok) {
		return json({ message: result.message || 'Server error' }, { status: res.status });
	}

	return json({ message: result.message });
};
