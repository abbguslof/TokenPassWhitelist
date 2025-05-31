import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

export const POST: RequestHandler = async ({ request }) => {
	const { password, username, inviter } = await request.json();

	const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD;
	const API_URL = process.env.VITE_API_URL;

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
