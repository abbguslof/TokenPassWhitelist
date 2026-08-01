import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';
import { env } from '$env/dynamic/private';

export const POST: RequestHandler = async ({ request, params }) => {
	const { password } = await request.json();
	const id = params.id;

	const API_URL = env.VITE_API_URL;
	const AUTH_TOKEN = env.AUTH_TOKEN;

	if (!API_URL || !AUTH_TOKEN) {
		return json({ message: 'Server config error' }, { status: 500 });
	}

	try {
		// Send to Velocity backend
		const res = await fetch(`${API_URL}/api/verify-permanent-password`, {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'X-Auth-Token': AUTH_TOKEN
			},
			body: JSON.stringify({ id, passwordHash: password }) // Note: we should hash it normally, but keeping simple
		});

		let result;
		try {
			result = await res.json();
		} catch (e) {
			result = { message: 'Invalid response from server' };
		}
		
		if (!res.ok) {
			return json({ message: result.message || 'Invalid password' }, { status: res.status });
		}

		return json({ message: 'Valid password' });
	} catch (e) {
		console.error(e);
		return json({ message: 'Failed to verify password' }, { status: 500 });
	}
};
