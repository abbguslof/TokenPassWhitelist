import { json } from '@sveltejs/kit';
import type { RequestHandler } from './$types';

const API_URL = process.env.VITE_API_URL!;
const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD!;

export const POST: RequestHandler = async ({ request }) => {
	const { password, username, inviter } = await request.json();

	if (!password || password !== ADMIN_PASSWORD) {
		return json({ message: 'Unauthorized' }, { status: 401 });
	}

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
};
