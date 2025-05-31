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
	let captchaLoaded = false;
	let captchaContainer: HTMLElement;

	// Global callback for hCaptcha
	if (typeof window !== 'undefined') {
		(window as any).onCaptchaComplete = (token: string) => {
			captchaToken = token;
		};

		(window as any).onCaptchaExpired = () => {
			captchaToken = '';
		};

		(window as any).onCaptchaError = (error: any) => {
			captchaToken = '';
		};
	}

	onMount(async () => {
		try {
			const res = await fetch(`/invite/${token}`);
			if (res.ok) {
				valid = true;
				// Initialize hCaptcha after validation succeeds and DOM is ready
				setTimeout(initializeCaptcha, 100);
			} else {
				error = "This invite link is invalid or expired.";
			}
		} catch (err) {
			error = "Failed to validate invite link.";
		}
	});

	function initializeCaptcha() {
		if (!captchaContainer) {
			return;
		}

		// Wait for hCaptcha script to load
		const checkHcaptcha = setInterval(() => {
			if (typeof (window as any).hcaptcha !== 'undefined') {
				clearInterval(checkHcaptcha);
				
				try {
					const siteKey = import.meta.env.VITE_HCAPTCHA_SITE_KEY;
					
					if (siteKey && siteKey !== 'undefined' && captchaContainer) {
						// Render the captcha using the element reference
						(window as any).hcaptcha.render(captchaContainer, {
							sitekey: siteKey,
							callback: 'onCaptchaComplete',
							'expired-callback': 'onCaptchaExpired',
							'error-callback': 'onCaptchaError'
						});
						captchaLoaded = true;
					} else {
						error = 'Configuration error: Missing captcha site key or container';
					}
				} catch (e) {
					error = 'Failed to load security verification';
				}
			}
		}, 100);

		// Timeout after 10 seconds
		setTimeout(() => {
			if (!captchaLoaded) {
				clearInterval(checkHcaptcha);
				error = 'Security verification failed to load. Please refresh the page.';
			}
		}, 10000);
	}

	async function submit() {
		if (!captchaToken) {
			error = "Please complete the CAPTCHA";
			return;
		}

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
                <div class="h-captcha" bind:this={captchaContainer}></div>
                {#if !captchaLoaded}
                    <p class="loading">Loading security verification...</p>
                {/if}
            </div>
            <button type="submit" disabled={submitting || !username || !captchaToken}>
                {submitting ? 'Processing...' : 'Join Server'}
            </button>
        </form>
    </div>
{:else}
    <div class="container">
        <p>Validating invite...</p>
    </div>
{/if}

<svelte:head>
    <script src="https://js.hcaptcha.com/1/api.js" async defer></script>
</svelte:head>

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
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
    }

    .loading {
        color: #666;
        font-size: 0.9rem;
        margin: 0;
    }
</style>
