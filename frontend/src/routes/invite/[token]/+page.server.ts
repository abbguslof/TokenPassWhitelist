const API_URL = process.env.VITE_API_URL;
if (!API_URL) throw new Error("Missing VITE_API_URL in environment (DOTENV FILE)");

const res = await fetch(`${API_URL}/api/check-token`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Auth-Token': process.env.VITE_API_SECRET || ''
  },
  body: JSON.stringify({ token })
});
