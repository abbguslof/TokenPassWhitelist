<script lang="ts">
    import { page } from '$app/stores';
    import { get } from 'svelte/store';

    let username = '';
    let message = '';
    let loading = false;

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
