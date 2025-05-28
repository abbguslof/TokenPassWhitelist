<script lang="ts">
    import { onMount } from 'svelte';
    import { page } from '$app/stores';
    import { browser } from '$app/environment';
  
    let username = '';
    let message = '';
    let loading = false;
  
    const submit = async () => {
      loading = true;
      message = '';
  
      const token = $page.params.token;
      try {
        const res = await fetch('http://localhost:5000/api/whitelist', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-Auth-Token': 'REPLACE_ME' // replace or pass from env
          },
          body: JSON.stringify({ token, username })
        });
  
        const data = await res.json();
        message = data.message;
      } catch (e) {
        message = 'Failed to submit request.';
      }
  
      loading = false;
    };
  </script>
  
  <h1>Whitelist Invitation</h1>
  <p>Enter your Minecraft username to accept the invite.</p>
  
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
  