<script lang="ts">
    import { page } from '$app/stores';
    import { get } from 'svelte/store';
  
    let username = '';
    let message = '';
    let success = false;
    let loading = false;
    const token = get(page).params.token;
  
    async function submit() {
      loading = true;
      message = '';
  
      const res = await fetch('/api/whitelist', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, username })
      });
  
      const data = await res.json();
      loading = false;
  
      if (!res.ok) {
        message = data.message || 'Something went wrong.';
      } else {
        success = true;
        message = data.message;
      }
    }
  </script>
  
  <svelte:head>
    <title>{import.meta.env.VITE_BRAND_NAME} - Invite</title>
  </svelte:head>
  
  <style>
    .container {
      max-width: 400px;
      margin: 5rem auto;
      padding: 2rem;
      border-radius: 8px;
      background: white;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
      font-family: sans-serif;
      text-align: center;
    }
    input, button {
      padding: 0.6rem;
      font-size: 1rem;
      margin-top: 1rem;
      width: 100%;
      box-sizing: border-box;
    }
    button {
      background-color: var(--primary);
      color: white;
      border: none;
      cursor: pointer;
    }
    .success {
      color: green;
      font-weight: bold;
    }
    .error {
      color: red;
      margin-top: 1rem;
    }
  </style>
  
  <div class="container" style="--primary: {import.meta.env.VITE_PRIMARY_COLOR}">
    {#if success}
      <h2>✅ Success!</h2>
      <p>You’ve been whitelisted.</p>
      <p style="margin-top: 1rem;">Server IP:</p>
      <code>{import.meta.env.VITE_SERVER_IP}</code>
    {:else}
      <h1>{import.meta.env.VITE_BRAND_NAME}</h1>
      <p>Enter your Minecraft username to accept your invite:</p>
  
      <input bind:value={username} placeholder="Minecraft Username" />
      <button on:click={submit} disabled={loading}>
        {loading ? 'Submitting...' : 'Submit'}
      </button>
  
      {#if message}
        <p class="error">{message}</p>
      {/if}
    {/if}
  </div>
  