<script lang="ts">
    import type { PageLoad } from './$types';
    import { page } from '$app/stores';
    import { get } from 'svelte/store';

    let username = '';
    let message = '';
    let loading = false;

    export const load: PageLoad = async ({ params, fetch }) => {
        const res = await fetch(`/api/check-token/${params.token}`);

        if (!res.ok) {
            return {
            status: 404,
            error: new Error('Invalid or expired invite link.')
            };
        }

        return { token: params.token };
    };

    const submit = async () => {
        loading = true;
        message = '';

        const token = get(page).params.token;

        try {
            const res = await fetch('/api/whitelist', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ token, username })
            });

            const data = await res.json();
            message = data.message ?? 'Unknown response.';
        } catch (e) {
            console.error(e);
        message = 'Failed to connect.';
        }
        loading = false;
    };
</script>
  
  <h1>Whitelist Invite</h1>
  <p>Enter your Minecraft username to accept the invite:</p>
  
  <input bind:value={username} placeholder="Username" />
  <button on:click={submit} disabled={loading}>
    {loading ? 'Submitting...' : 'Submit'}
  </button>
  
  {#if message}
    <p>{message}</p>
  {/if}
  
  <style>
    input, button {
      padding: 0.5rem;
      margin-top: 1rem;
      font-size: 1rem;
    }
  </style>
