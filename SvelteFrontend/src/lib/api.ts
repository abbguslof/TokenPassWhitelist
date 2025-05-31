export async function whitelistUser(token: string, username: string) {
	const res = await fetch(`${import.meta.env.VITE_API_URL}/api/whitelist`, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
			'X-Auth-Token': import.meta.env.VITE_AUTH_TOKEN
		},
		body: JSON.stringify({ token, username })
	});
	return await res.json();
}

export async function checkToken(token: string) {
	const res = await fetch(`${import.meta.env.VITE_API_URL}/api/check-token`, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
			'X-Auth-Token': import.meta.env.VITE_AUTH_TOKEN
		},
		body: JSON.stringify({ token })
	});
	return await res.json();
}
