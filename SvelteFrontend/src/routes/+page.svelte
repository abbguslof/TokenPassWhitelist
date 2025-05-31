<script>
    const brandName = import.meta.env.VITE_BRAND_NAME;
</script>

<main class="container">
    <div class="hero-section">
        <h1>👋 Welcome to {brandName}</h1>
        
        <div class="description">
            <p class="primary-text">Join our exclusive Minecraft community</p>
            <p class="secondary-text">We use a secure invite-based whitelist system to ensure a trusted, friendly environment for all players.</p>
        </div>
    </div>

    <div class="how-it-works">
        <h3>How to Join</h3>
        <p class="simple-instructions">
            To join our server, you'll need an invite from a current member. Once you have an invite link, 
            use it to register your Minecraft username and get whitelisted on our server.
        </p>
    </div>

    <div class="cta-section">
        <div class="invite-prompt">
            <h3>Already have an invite?</h3>
            <p>Use your invite link to complete registration and join the server.</p>
        </div>
    </div>
</main>

<style>
    .container {
        max-width: 600px;
        margin: 3rem auto;
        padding: 3rem 2rem;
        font-family: system-ui, sans-serif;
        background: white;
        border-radius: 10px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
        animation: fadeIn 0.8s ease-out;
    }

    @keyframes fadeIn {
        from {
            opacity: 0;
            transform: translateY(30px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    .hero-section {
        text-align: center;
        margin-bottom: 3rem;
    }

    h1 {
        font-size: 2.5rem;
        margin-bottom: 2rem;
        color: #333;
        font-weight: 600;
    }

    .description {
        margin-bottom: 3rem;
    }

    .primary-text {
        font-size: 1.25rem;
        color: #333;
        margin-bottom: 0.75rem;
        font-weight: 500;
    }

    .secondary-text {
        font-size: 1rem;
        color: #666;
        line-height: 1.6;
        margin: 0;
    }

    .how-it-works {
        margin-bottom: 3rem;
        text-align: center;
    }

    .how-it-works h3 {
        font-size: 1.25rem;
        margin-bottom: 0.75rem;
        color: #666;
        font-weight: 500;
    }

    .simple-instructions {
        color: #666;
        font-size: 1rem;
        line-height: 1.6;
        margin: 0;
        max-width: 400px;
        margin: 0 auto;
    }

    .cta-section {
        text-align: center;
        background: #f0f8ff;
        padding: 2rem;
        border-radius: 8px;
        border: 1px solid #d4e7ff;
    }

    .invite-prompt h3 {
        font-size: 1.5rem;
        margin-bottom: 0.75rem;
        color: #333;
    }

    .invite-prompt p {
        color: #666;
        margin: 0;
        line-height: 1.5;
    }

    /* Responsive design */
    @media (max-width: 640px) {
        .container {
            margin: 1rem;
            padding: 2rem 1.5rem;
        }

        h1 {
            font-size: 2rem;
        }

        .cta-section {
            padding: 1.5rem;
        }
    }
</style>
