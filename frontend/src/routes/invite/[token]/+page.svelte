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
		const res = await fetch(`/invite/${token}`);
		if (res.ok) valid = true;
		else error = "This invite link is invalid or expired.";
	});

	async function submit() {
		submitting = true;
		error = null;

		const res = await fetch(`/invite/${token}/whitelist`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ username, captchaToken })
		});

		const data = await res.json();
		if (res.ok) goto('/success');
		else error = data.message || "Error";
		submitting = false;
	}
</script>

{#if error}
	<p class="error">{error}</p>
{:else if valid}
	<h1>You're invited to {import.meta.env.VITE_BRAND_NAME}</h1>
	<form on:submit|preventDefault={submit}>
		<input type="text" placeholder="Minecraft username" bind:value={username} required />
		<div class="captcha">
			<script src="https://js.hcaptcha.com/1/api.js" async defer></script>
			<div
				class="h-captcha"
				data-sitekey={import.meta.env.VITE_HCAPTCHA_SITE_KEY}
				data-callback={(token) => (captchaToken = token)}
			/>
		</div>
		<button disabled={submitting}>Join</button>
	</form>
{/if}

<style>
	form { display: flex; flex-direction: column; gap: 1rem; max-width: 400px; margin: auto; }
	input, button { padding: 0.75rem; font-size: 1rem; }
	.error { color: red; text-align: center; margin-top: 2rem; }
</style>
