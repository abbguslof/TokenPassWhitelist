<script>
    const brandName = import.meta.env.VITE_BRAND_NAME;
    const serverIP = import.meta.env.VITE_SERVER_IP;
</script>

<main class="container">
    <div class="success-icon">
        <div class="checkmark">✓</div>
    </div>
    
    <h1>🎉 Welcome to {brandName}!</h1>
    
    <div class="success-message">
        <p class="primary-text">You've been successfully added to the whitelist!</p>
        <p class="secondary-text">Your adventure awaits in our trusted community.</p>
    </div>

    <div class="server-info">
        <h2>Join the Server</h2>
        <div class="server-ip">
            <span class="label">Server IP:</span>
            <code class="ip-address">{serverIP}</code>
        </div>
        <button class="copy-button" on:click={() => {
            navigator.clipboard.writeText(serverIP);
            // Simple feedback (you could enhance this with a toast notification)
            const btn = event.target;
            const originalText = btn.textContent;
            btn.textContent = 'Copied!';
            setTimeout(() => btn.textContent = originalText, 2000);
        }}>
            📋 Copy Server IP
        </button>
    </div>

    <div class="next-steps">
        <h3>What's Next?</h3>
        <ul>
            <li>Launch Minecraft and add the server to your server list</li>
            <li>Connect using the server IP above</li>
            <li>Enjoy building and exploring with the community!</li>
        </ul>
    </div>
</main>

<style>
    .container {
        max-width: 600px;
        margin: 3rem auto;
        text-align: center;
        padding: 3rem 2rem;
        font-family: system-ui, sans-serif;
        background: white;
        border-radius: 10px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
        animation: slideIn 0.6s ease-out;
    }

    @keyframes slideIn {
        from {
            opacity: 0;
            transform: translateY(20px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }

    .success-icon {
        margin-bottom: 2rem;
    }

    .checkmark {
        display: inline-block;
        width: 80px;
        height: 80px;
        background: linear-gradient(135deg, #4CAF50, #45a049);
        border-radius: 50%;
        color: white;
        font-size: 2.5rem;
        font-weight: bold;
        line-height: 80px;
        box-shadow: 0 4px 20px rgba(76, 175, 80, 0.3);
        animation: bounce 0.8s ease-out 0.3s;
        transform: scale(0);
        animation-fill-mode: forwards;
    }

    @keyframes bounce {
        0% { transform: scale(0); }
        50% { transform: scale(1.1); }
        100% { transform: scale(1); }
    }

    h1 {
        font-size: 2.5rem;
        margin-bottom: 2rem;
        color: #333;
        font-weight: 600;
    }

    .success-message {
        margin-bottom: 2.5rem;
    }

    .primary-text {
        font-size: 1.25rem;
        color: #333;
        margin-bottom: 0.5rem;
        font-weight: 500;
    }

    .secondary-text {
        font-size: 1rem;
        color: #666;
        margin: 0;
    }

    .server-info {
        background: #f8f9fa;
        padding: 2rem;
        border-radius: 8px;
        margin-bottom: 2rem;
        border: 1px solid #e9ecef;
    }

    .server-info h2 {
        font-size: 1.5rem;
        margin-bottom: 1rem;
        color: #333;
    }

    .server-ip {
        margin-bottom: 1.5rem;
    }

    .label {
        display: block;
        font-size: 0.9rem;
        color: #666;
        margin-bottom: 0.5rem;
        font-weight: 500;
    }

    .ip-address {
        display: inline-block;
        background: #fff;
        padding: 0.75rem 1.5rem;
        border: 2px solid #007cba;
        border-radius: 6px;
        font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
        font-size: 1.1rem;
        font-weight: 600;
        color: #007cba;
        letter-spacing: 0.5px;
    }

    .copy-button {
        background: #007cba;
        color: white;
        border: none;
        padding: 0.75rem 1.5rem;
        border-radius: 6px;
        font-size: 1rem;
        cursor: pointer;
        transition: all 0.2s ease;
        font-weight: 500;
    }

    .copy-button:hover {
        background: #0056a3;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(0, 124, 186, 0.3);
    }

    .copy-button:active {
        transform: translateY(0);
    }

    .next-steps {
        text-align: left;
        background: #fff8e1;
        padding: 2rem;
        border-radius: 8px;
        border-left: 4px solid #ffc107;
    }

    .next-steps h3 {
        font-size: 1.25rem;
        margin-bottom: 1rem;
        color: #333;
        text-align: center;
    }

    .next-steps ul {
        margin: 0;
        padding-left: 1.5rem;
        color: #555;
    }

    .next-steps li {
        margin-bottom: 0.75rem;
        line-height: 1.5;
    }

    .next-steps li:last-child {
        margin-bottom: 0;
    }

    /* Responsive adjustments */
    @media (max-width: 640px) {
        .container {
            margin: 1rem;
            padding: 2rem 1.5rem;
        }

        h1 {
            font-size: 2rem;
        }

        .checkmark {
            width: 60px;
            height: 60px;
            font-size: 2rem;
            line-height: 60px;
        }

        .server-info, .next-steps {
            padding: 1.5rem;
        }
    }
</style>
