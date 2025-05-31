<script>
    const brand = import.meta.env.VITE_BRAND_NAME;
    const ip = import.meta.env.VITE_SERVER_IP;
</script>

<main>
    <h1>👋 Welcome to {brand}</h1>
    <p>This server uses a secure invite-based whitelist system to ensure a trusted community.</p>
    <p>To join, you'll need to be invited by a current player.</p>
    <p>Already have an invite link? Use the full link.</p>
</main>

<style>
    main {
        max-width: 600px;
        margin: 5rem auto;
        text-align: center;
        padding: 2rem;
        font-family: system-ui, sans-serif;
        background: white;
        border-radius: 10px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
    }

    h1 {
        font-size: 2.25rem;
        margin-bottom: 1rem;
    }
</style>
