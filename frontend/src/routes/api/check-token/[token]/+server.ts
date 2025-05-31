export async function GET({ params }) {
    const res = await fetch(`${process.env.VITE_API_URL}/api/check-token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Auth-Token': process.env.AUTH_TOKEN!
      },
      body: JSON.stringify({ token: params.token })
    });
  
    return new Response(res.body, { status: res.status });
  }
  