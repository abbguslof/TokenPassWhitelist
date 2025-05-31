<script lang="ts">
	import { onMount } from 'svelte';
	let password = '';
	let username = '';
	let inviter = '';
	let result: string | null = null;
	let error: string | null = null;
	let submitting = false;
</script>

<main>
	<h1>🛡️ Admin Invite Panel</h1>

	<form on:submit|preventDefault={async () => {
		result = error = null;
		submitting = true;

		const res = await fetch('/admin/create', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ password, username, inviter })
		});

		const data = await res.json();
		if (res.ok) {
			result = data.link;
		} else {
			error = data.message || 'Error creating invite.';
		}
		submitting = false;
	}}>
		<input type="password" placeholder="Admin Password" bind:value={password} required />
		<input type="text" placeholder="Target Minecraft Username" bind:value={username} required />
		<input type="text" placeholder="Inviter Name (optional)" bind:value={inviter} />
		<button disabled={submitting}>Generate Invite</button>
	</form>

	{#if result}
		<p class="result">✅ Invite Created: <a href={result} target="_blank">{result}</a></p>
	{:else if error}
		<p class="error">❌ {error}</p>
	{/if}
</main>

<style>
	main {
		max-width: 500px;
		margin: 4rem auto;
		padding: 2rem;
		background: white;
		border-radius: 10px;
		box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
	}

	form {
		display: flex;
		flex-direction: column;
		gap: 1rem;
		margin-top: 2rem;
	}

	input, button {
		padding: 0.75rem;
		font-size: 1rem;
	}

	.result, .error {
		margin-top: 1rem;
		text-align: center;
	}
	.result a {
		word-break: break-all;
	}
</style>
