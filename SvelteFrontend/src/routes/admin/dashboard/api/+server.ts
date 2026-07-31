import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

export const POST: RequestHandler = async ({ request }) => {
	const { action, password, payload } = await request.json();
	const API_URL = process.env.VITE_API_URL;

	if (!API_URL) return json({ message: 'Server config error' }, { status: 500 });

	let url = `${API_URL}/api/${action}`;
	let method = 'POST';
	if (action === 'invites' || action === 'players') method = 'GET';
	
	try {
		const res = await fetch(url, {
			method,
			headers: {
				'Content-Type': 'application/json',
				'X-Admin-Password': password
			},
			body: method === 'POST' ? JSON.stringify(payload || {}) : undefined
		});
		
		const data = await res.json();
		return json(data, { status: res.status });
	} catch (e) {
		return json({ message: 'Failed to contact backend server.' }, { status: 500 });
	}
};
