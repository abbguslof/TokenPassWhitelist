import { json } from '@sveltejs/kit';

export async function POST({ request }) {
  const { username, password } = await request.json();

  const res = await fetch(`${process.env.VITE_API_URL}/api/invite-admin`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Admin-Password': password
    },
    body: JSON.stringify({ username, inviterName: 'WebAdmin' })
  });

  const data = await res.json();
  return json(data, { status: res.status });
}
