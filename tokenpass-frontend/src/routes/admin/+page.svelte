<script>
    let token = "";
    let response = null;
    let error = null;
    let submitting = false;
    let password = "";
  
    async function submit() {
      submitting = true;
      response = null;
      error = null;
  
      try {
        const res = await fetch("/api/invite-admin", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-Admin-Password": password
          }
        });
  
        const data = await res.json();
  
        if (!res.ok) {
          error = data.message || "Something went wrong.";
        } else {
          response = data;
          token = data.token;
        }
      } catch (e) {
        error = "Failed to connect to the server.";
      } finally {
        submitting = false;
      }
    }
  </script>
  
  <style>
    .container {
      max-width: 600px;
      margin: 2rem auto;
      padding: 1rem;
      font-family: sans-serif;
    }
  
    input, button {
      padding: 0.6rem;
      font-size: 1rem;
      margin-top: 0.5rem;
      width: 100%;
      box-sizing: border-box;
    }
  
    .link {
      word-break: break-word;
      margin-top: 1rem;
      font-weight: bold;
      color: green;
    }
  
    .error {
      color: red;
      margin-top: 1rem;
    }
  
    @media (max-width: 600px) {
      .container {
        padding: 1rem;
      }
    }
  </style>
  
  <div class="container">
    <h2>Generate Invite (Admin)</h2>
  
    <label>
      Admin Password:
      <input type="password" bind:value={password} />
    </label>
  
    <button on:click={submit} disabled={submitting}>
      {submitting ? "Submitting..." : "Generate Invite"}
    </button>
  
    {#if token}
      <div class="link">
        Invite Link:<br />
        <a href={`/invite/${token}`} target="_blank">{window.location.origin}/invite/{token}</a>
      </div>
    {/if}
  
    {#if error}
      <div class="error">{error}</div>
    {/if}
  </div>
  