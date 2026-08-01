<script lang="ts">
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { onMount } from 'svelte';

	/**
	 * State Management
	 * id: The unique permanent link ID extracted from the route parameter
	 * hasPassword: True if the backend indicates this link is password-protected
	 */
	let username = '';
	let password = '';
	let error: string | null = null;
	let submitting = false;
	let valid = false;
	let verifyingPassword = false;
	let hasPassword = false;
	let step = 1;
	let creatorName = '';
	let id = $page.params.id;
	let captchaToken = '';
	let captchaLoaded = false;
	let captchaContainer: HTMLElement;

	if (typeof window !== 'undefined') {
		(window as any).onCaptchaComplete = (token: string) => { captchaToken = token; };
		(window as any).onCaptchaExpired = () => { captchaToken = ''; };
		(window as any).onCaptchaError = () => { captchaToken = ''; };
	}

	onMount(async () => {
		try {
			const res = await fetch(`/validate-permanent-link/${id}`);
			if (res.ok) {
				const data = await res.json();
				valid = true;
				hasPassword = data.hasPassword;
				step = data.hasPassword ? 1 : 2;
				creatorName = data.creatorName;
				if (step === 2) {
					setTimeout(initializeCaptcha, 100);
				}
			} else {
				const data = await res.json();
				error = data.message || "Invalid or expired permanent link.";
			}
		} catch {
			error = "Failed to validate link.";
		}
	});

	/**
	 * Initializes the hCaptcha widget. 
	 * It polls for the global hcaptcha object since the script is loaded asynchronously.
	 */
	async function nextStep() {
		if (!password) return;
		
		verifyingPassword = true;
		error = null;

		try {
			const res = await fetch(`/verify-permanent-password/${id}`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ password })
			});

			if (res.ok) {
				step = 2;
				setTimeout(initializeCaptcha, 100);
			} else {
				const data = await res.json();
				error = data.message || 'Invalid password';
			}
		} catch {
			error = 'Network error while verifying password';
		} finally {
			verifyingPassword = false;
		}
	}

	function initializeCaptcha() {
		if (!captchaContainer) return;

		const check = setInterval(() => {
			if (typeof (window as any).hcaptcha !== 'undefined') {
				clearInterval(check);
				try {
					const siteKey = import.meta.env.VITE_HCAPTCHA_SITE_KEY;
					if (siteKey && siteKey !== 'undefined') {
						(window as any).hcaptcha.render(captchaContainer, {
							sitekey: siteKey,
							callback: 'onCaptchaComplete',
							'expired-callback': 'onCaptchaExpired',
							'error-callback': 'onCaptchaError'
						});
						captchaLoaded = true;
					} else {
						error = 'Missing captcha site key';
					}
				} catch {
					error = 'Failed to load CAPTCHA';
				}
			}
		}, 100);

		setTimeout(() => {
			if (!captchaLoaded) {
				clearInterval(check);
				error = 'CAPTCHA failed to load. Refresh the page.';
			}
		}, 10000);
	}

	/**
	 * Submits the form data to the local +server.ts proxy.
	 * Includes the captchaToken, username, and conditionally the password.
	 */
	async function submit() {
		if (!captchaToken) {
			error = "Please complete the CAPTCHA";
			return;
		}

		submitting = true;
		error = null;

		try {
			const res = await fetch(`/validate-permanent-link/${id}`, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify({ username, captchaToken, password })
			});

			const data = await res.json();
			if (res.ok) {
				goto('/success');
			} else {
				error = data.message || 'Submission failed';
			}
		} catch {
			error = 'Network error';
		} finally {
			submitting = false;
		}
	}
</script>

{#if error}
	<div class="container"><p class="error">{error}</p></div>
{:else if valid}
	<div class="container">
		<h1>Join {import.meta.env.VITE_BRAND_NAME}</h1>
		<p class="subtitle">Invited by <strong>{creatorName}</strong></p>
		
		{#if step === 1}
			<form on:submit|preventDefault={nextStep}>
				<p>This invite is password-protected.</p>
				<input type="password" placeholder="Invite Password" bind:value={password} required disabled={verifyingPassword} />
				<button type="submit" disabled={verifyingPassword}>
					{verifyingPassword ? 'Verifying...' : 'Verify Password'}
				</button>
			</form>
		{:else}
			<form on:submit|preventDefault={submit}>
				<input type="text" placeholder="Minecraft Username" bind:value={username} required title="Valid Minecraft username" disabled={submitting} />
				
				<div class="captcha">
					<div class="h-captcha" bind:this={captchaContainer}></div>
					{#if !captchaLoaded}
						<p class="loading">Loading CAPTCHA...</p>
					{/if}
				</div>
				
				<button type="submit" disabled={submitting || !username || !captchaToken}>
					{submitting ? 'Processing...' : 'Join Server'}
				</button>
			</form>
		{/if}
	</div>
{:else}
	<div class="container"><p>Validating link...</p></div>
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
		background: white;
		border-radius: 10px;
		box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
		font-family: system-ui, sans-serif;
	}
	.subtitle {
		color: #666;
		margin-bottom: 2rem;
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
	@media (max-width: 640px) {
		.container {
			margin: 1rem;
			padding: 1.5rem 1rem;
		}
		form {
			width: 100%;
		}
	}
</style>
