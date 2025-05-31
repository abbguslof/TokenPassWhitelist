<script>
    const brand = import.meta.env.VITE_BRAND_NAME;
  </script>
  
  <svelte:head>
    <title>TokenPassWhitelist</title>
  </svelte:head>
  
  <style>
    main {
      max-width: 600px;
      margin: 5rem auto;
      text-align: center;
      padding: 2rem;
      font-family: sans-serif;
      background: white;
      border-radius: 10px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.05);
    }
  
    h1 {
      font-size: 2.25rem;
      font-weight: bold;
      margin-bottom: 1rem;
    }
  
    p {
      color: #555;
      margin-top: 1rem;
    }
  </style>
  
  <main>
    <h1>👋 Welcome to TokenPassWhitelist</h1>
    <p>TokenPass is a secure invitation system that lets trusted players generate one-time use links to join private Minecraft servers.</p>
    <p>If you received an invite link to {brand}, please use the full link to proceed.</p>
  </main>
  