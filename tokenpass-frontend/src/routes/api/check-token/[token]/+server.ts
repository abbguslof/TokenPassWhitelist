import { json } from '@sveltejs/kit';

export async function GET({ params }) {
  const token = params.token;

  const res = await fetch(`${process.env.VITE_API_URL}/api/check-token`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Auth-Token': process.env.AUTH_TOKEN!
    },
    body: JSON.stringify({ token })
  });

  const data = await res.json();
  return json(data, { status: res.status });
}
