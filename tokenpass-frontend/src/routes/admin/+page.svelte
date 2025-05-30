<script lang="ts">
    let password = '';
    let username = '';
    let message = '';
    let link = '';
    let submitting = false;
  
    const submit = async () => {
      submitting = true;
      message = '';
      link = '';
  
      try {
        const res = await fetch('/api/invite-admin', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username, password })
        });
  
        const data = await res.json();
  
        if (!res.ok) {
          message = data.message ?? 'Something went wrong.';
        } else {
          message = 'Invite created!';
          link = data.link;
        }
      } catch (e) {
        message = 'Failed to connect to server.';
      } finally {
        submitting = false;
      }
    };
  </script>
  
  <h1>Admin: Generate Invite</h1>
  
  <label>
    Username to invite:
    <input bind:value={username} />
  </label>
  
  <label>
    Admin Password:
    <input type="password" bind:value={password} />
  </label>
  
  <button on:click={submit} disabled={submitting}>
    {submitting ? 'Generating...' : 'Generate Invite'}
  </button>
  
  {#if message}
    <p>{message}</p>
  {/if}
  
  {#if link}
    <p><strong>Invite Link:</strong> <a href={link} target="_blank">{link}</a></p>
  {/if}
  
  <style>
    input, button {
      display: block;
      width: 100%;
      margin-top: 0.5rem;
      padding: 0.5rem;
      font-size: 1rem;
    }
  
    p {
      margin-top: 1rem;
      font-weight: bold;
    }
  
    a {
      color: green;
    }
  </style>  