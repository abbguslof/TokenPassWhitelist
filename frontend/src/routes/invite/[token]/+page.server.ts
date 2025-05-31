const API_BASE = process.env.VITE_API_BASE;
if (!API_BASE) throw new Error("Missing VITE_API_BASE in environment");

const res = await fetch(`${API_BASE}/api/check-token`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Auth-Token': process.env.VITE_API_SECRET || ''
  },
  body: JSON.stringify({ token })
});
