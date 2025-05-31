<script lang="ts">
    let username = '';
    let password = '';
    let message = '';
    let link = '';
    let loading = false;
  
    async function submit() {
      loading = true;
      message = '';
      link = '';
  
      const res = await fetch('/api/invite-admin', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
  
      const data = await res.json();
      loading = false;
  
      if (!res.ok) {
        message = data.message || 'Something went wrong.';
      } else {
        message = 'Invite created!';
        link = data.link;
      }
    }
  </script>
  
  <svelte:head>
    <title>Admin – {import.meta.env.VITE_BRAND_NAME}</title>
  </svelte:head>
  
  <style>
    .container {
      max-width: 400px;
      margin: 5rem auto;
      padding: 2rem;
      border-radius: 8px;
      background: white;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
      text-align: center;
      font-family: sans-serif;
    }
    input, button {
      width: 100%;
      padding: 0.6rem;
      margin-top: 1rem;
      font-size: 1rem;
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
    a {
      word-break: break-all;
      font-size: 0.9rem;
    }
  </style>
  
  <div class="container" style="--primary: {import.meta.env.VITE_PRIMARY_COLOR}">
    <h2>Admin Invite Generator</h2>
  
    <input bind:value={username} placeholder="Username to invite" />
    <input bind:value={password} type="password" placeholder="Admin Password" />
  
    <button on:click={submit} disabled={loading}>
      {loading ? 'Submitting...' : 'Generate Invite'}
    </button>
  
    {#if message}
      <p class={link ? 'success' : 'error'}>{message}</p>
    {/if}
  
    {#if link}
      <a href={link} target="_blank">{link}</a>
    {/if}
  </div>
  