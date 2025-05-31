<script lang="ts">
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';
	
	let username = '';
	let error: string | null = null;
	let submitting = false;
	let valid = false;
	let token = $page.params.token;
	let captchaToken = '';

	onMount(async () => {
		try {
			const res = await fetch(`/invite/${token}`);
			if (res.ok) {
				valid = true;
			} else {
				error = "This invite link is invalid or expired.";
			}
		} catch (err) {
			error = "Failed to validate invite link.";
		}
	});

	async function submit() {
		submitting = true;
		error = null;

		try {
			const res = await fetch(`/invite/${token}`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json'
				},
				body: JSON.stringify({ username, captchaToken })
			});

			const data = await res.json();
			if (res.ok) {
				goto('/success');
			} else {
				error = data.message || 'Failed to process invite';
			}
		} catch (err) {
			error = 'Network error occurred';
		} finally {
			submitting = false;
		}
	}
</script>

{#if error}
    <div class="container">
        <p class="error">{error}</p>
    </div>
{:else if valid}
    <div class="container">
        <h1>You're invited to {import.meta.env.VITE_BRAND_NAME}</h1>
        <form on:submit|preventDefault={submit}>
            <input 
                type="text" 
                placeholder="Minecraft Username" 
                bind:value={username} 
                required 
                disabled={submitting}
            />
            <div class="captcha">
                <script src="https://js.hcaptcha.com/1/api.js" async defer></script>
                <div
                    class="h-captcha"
                    data-sitekey={import.meta.env.VITE_HCAPTCHA_SITE_KEY}
                    data-callback="onCaptchaComplete"
                ></div>
            </div>
            <button type="submit" disabled={submitting || !username}>
                {submitting ? 'Processing...' : 'Join Server'}
            </button>
        </form>
    </div>
{:else}
    <div class="container">
        <p>Validating invite...</p>
    </div>
{/if}

<style>
    .container {
        max-width: 600px;
        margin: 5rem auto;
        text-align: center;
        padding: 2rem;
        font-family: system-ui, sans-serif;
        background: white;
        border-radius: 10px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
    }
    
    form { 
        display: flex; 
        flex-direction: column; 
        gap: 1rem; 
        max-width: 400px; 
        margin: auto; 
    }
    
    input, button { 
        padding: 0.75rem; 
        font-size: 1rem; 
        border: 1px solid #ccc;
        border-radius: 4px;
    }
    
    button {
        background: #007cba;
        color: white;
        cursor: pointer;
    }
    
    button:disabled {
        background: #ccc;
        cursor: not-allowed;
    }
    
    .error { 
        color: red; 
        text-align: center; 
        margin-top: 2rem; 
    }
    
    .captcha {
        display: flex;
        justify-content: center;
    }
</style>
