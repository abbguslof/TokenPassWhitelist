import { json } from '@sveltejs/kit';

export async function POST({ request }) {
  const { token, username } = await request.json();

  const response = await fetch(`${process.env.VITE_API_URL}/api/whitelist`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Auth-Token': process.env.AUTH_TOKEN!
    },
    body: JSON.stringify({ token, username })
  });

  const data = await response.json();
  return json(data, { status: response.status });
}
